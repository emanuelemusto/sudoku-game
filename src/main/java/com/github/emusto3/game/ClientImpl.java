package com.github.emusto3.game;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;

import com.github.emusto3.beans.Challenge;
import com.github.emusto3.beans.Pair;
import com.github.emusto3.beans.Player;
import com.github.emusto3.interfaces.Client;

import com.github.emusto3.exceptions.*;
import com.github.emusto3.interfaces.*;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FutureRemove;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class ClientImpl implements Client {
    
    // Constants
    private final int DEFAULT_MASTER_PORT = 4000;
    private final Number160 PLAYERS_KEY = Number160.createHash("players");
    private final Number160 CHALLENGES_KEY = Number160.createHash("challenges");
    
    // Network components
    private final Peer peer;
    private final PeerDHT dht;
    
    // Game state
    private ArrayList<Challenge> challenges = new ArrayList<>();
    private ArrayList<Player> players = new ArrayList<>();
    private Challenge currentChallenge = null;
    private Player currentPlayer = null;
    
    // Legacy field kept for compatibility
    private final ArrayList<String> s_topics = new ArrayList<>();

    /**
     * Constructor - Initializes the P2P client and connects to the master peer
     */
    public ClientImpl(String masterPeerAddress, int peerId, final MessageListener messageListener) throws Exception {
        this.peer = new PeerBuilder(Number160.createHash(peerId))
                .ports(DEFAULT_MASTER_PORT + peerId)
                .start();
        this.dht = new PeerBuilderDHT(peer).start();

        connectToMasterPeer(masterPeerAddress);
        setupMessageHandler(messageListener);
        initializeDHTStructures();
    }

    /**
     * Connects to the master peer in the network
     */
    private void connectToMasterPeer(String masterPeerAddress) throws Exception {
        FutureBootstrap bootstrapFuture = peer.bootstrap()
                .inetAddress(InetAddress.getByName(masterPeerAddress))
                .ports(DEFAULT_MASTER_PORT)
                .start();
        bootstrapFuture.awaitUninterruptibly();

        if (bootstrapFuture.isSuccess()) {
            peer.discover()
                    .peerAddress(bootstrapFuture.bootstrapTo().iterator().next())
                    .start()
                    .awaitUninterruptibly();
        } else {
            throw new MasterPeerNotFoundException();
        }
    }

    /**
     * Sets up the message handler for incoming P2P messages
     */
    private void setupMessageHandler(final MessageListener messageListener) {
        peer.objectDataReply(new ObjectDataReply() {
            public Object reply(PeerAddress sender, Object request) throws Exception {
                return messageListener.parseMessage(request);
            }
        });
    }

    /**
     * Initializes the DHT structures for players and challenges
     */
    private void initializeDHTStructures() throws Exception {
        initializeDHTKey(PLAYERS_KEY, players);
        initializeDHTKey(CHALLENGES_KEY, challenges);
    }

    /**
     * Helper method to initialize a DHT key with default data if it doesn't exist
     */
    private void initializeDHTKey(Number160 key, Object defaultData) throws Exception {
        try {
            dht.get(key).start().awaitUninterruptibly();
        } catch (Exception e) {
            dht.put(key).data(new Data(defaultData)).start().awaitUninterruptibly();
        }
    }

    @Override
    public boolean checkPlayer(String nickname) throws Exception {
        if (currentPlayer != null) {
            throw new RuntimeException("Player già presente.");
        }

        if (nickname.contains(" ")) {
            return false;
        }

        return registerPlayer(nickname);
    }

    /**
     * Registers a new player if the nickname is available
     */
    private boolean registerPlayer(String nickname) throws Exception {
        try {
            FutureGet futureGet = dht.get(PLAYERS_KEY).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (!futureGet.isEmpty()) {
                    players = (ArrayList<Player>) futureGet.dataMap().values().iterator().next().object();

                    if (isNicknameAlreadyUsed(nickname)) {
                        return false;
                    }
                }

                currentPlayer = new Player(nickname, peer.peerAddress());
                players.add(currentPlayer);
                dht.put(PLAYERS_KEY).data(new Data(players)).start().awaitUninterruptibly();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Checks if a nickname is already in use
     */
    private boolean isNicknameAlreadyUsed(String nickname) {
        return players.stream()
                .anyMatch(player -> player.getNickname().equals(nickname));
    }

    @Override
    public boolean generateNewSudoku(String gameCode, int seed) throws Exception {
        try {
            currentChallenge = new Challenge(gameCode, currentPlayer.getNickname(), seed);

            FutureGet futureGet = dht.get(Number160.createHash(gameCode)).start().awaitUninterruptibly();
            
            if (futureGet.isSuccess() && checkChallenge(gameCode)) {
                if (!futureGet.isEmpty()) {
                    return false; // Challenge already exists
                }

                dht.put(Number160.createHash(gameCode))
                        .data(new Data(currentChallenge))
                        .start()
                        .awaitUninterruptibly();

                return addChallengeToList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Adds the current challenge to the global challenges list
     */
    private boolean addChallengeToList() throws Exception {
        try {
            FutureGet futureGet = dht.get(CHALLENGES_KEY).start().awaitUninterruptibly();
            
            if (futureGet.isSuccess()) {
                challenges.add(currentChallenge);
                dht.put(CHALLENGES_KEY).data(new Data(challenges)).start().awaitUninterruptibly();
                
                reloadPlayers();
                notifyAllPlayersAboutChallengeUpdate();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Notifies all players about challenge list updates
     */
    private void notifyAllPlayersAboutChallengeUpdate() throws Exception {
        for (Player player : players) {
            if (player.getNickname().equals(currentPlayer.getNickname())) {
                continue;
            }
            
            FutureDirect futureDirect = dht.peer()
                    .sendDirect(player.getPeerAdd())
                    .object(challenges)
                    .start();
            futureDirect.awaitUninterruptibly();
        }
    }

    @Override
    public boolean checkChallenge(String gameCode) throws Exception {
        try {
            FutureGet futureGet = dht.get(CHALLENGES_KEY).start().awaitUninterruptibly();
            
            if (futureGet.isSuccess()) {
                if (!futureGet.isEmpty()) {
                    challenges = (ArrayList<Challenge>) futureGet.dataMap().values().iterator().next().object();
                }

                if (isChallengeCodeAlreadyUsed(gameCode)) {
                    throw new ChallengeAlreadyExistsException();
                }

                return true;
            }
        } catch (ChallengeAlreadyExistsException e) {
            System.out.println("Sfida con codice partita " + gameCode + " già esistente");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a challenge code is already in use
     */
    private boolean isChallengeCodeAlreadyUsed(String gameCode) {
        return challenges.stream()
                .anyMatch(challenge -> challenge.getCodice_partita().equals(gameCode));
    }

    @Override
    public boolean removeChallenge(String gameCode) throws Exception {
        try {
            FutureRemove futureRemove = dht.remove(Number160.createHash(gameCode))
                    .all()
                    .start()
                    .awaitUninterruptibly();

            return futureRemove.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void removeFromChallengeList() throws Exception {
        try {
            FutureGet futureGet = dht.get(CHALLENGES_KEY).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (!futureGet.isEmpty()) {
                    challenges = (ArrayList<Challenge>) futureGet.dataMap().values().iterator().next().object();
                    
                    if (challenges.size() < 2) {
                        challenges.clear();
                    } else {
                        challenges.remove(findCurrentChallengeIndex());
                    }

                    dht.put(CHALLENGES_KEY).data(new Data(challenges)).start().awaitUninterruptibly();
                }
                
                reloadPlayers();
                notifyAllPlayersAboutChallengeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reloadChallengeList() throws Exception {
        try {
            FutureGet futureGet = dht.get(CHALLENGES_KEY).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (!futureGet.isEmpty()) {
                    challenges = (ArrayList<Challenge>) futureGet.dataMap().values().iterator().next().object();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reloadPlayers() throws Exception {
        try {
            FutureGet futureGet = dht.get(PLAYERS_KEY).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (!futureGet.isEmpty()) {
                    players = (ArrayList<Player>) futureGet.dataMap().values().iterator().next().object();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateChallengeList() throws Exception {
        int challengeIndex = findCurrentChallengeIndex();

        if (challengeIndex == -1) {
            throw new ChallengeNotFoundException();
        }

        try {
            FutureGet futureGet = dht.get(CHALLENGES_KEY).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (!futureGet.isEmpty()) {
                    challenges.get(challengeIndex)
                            .setPlayers_scores(currentChallenge.getPlayers_scores());
                }
            }
            
            dht.put(CHALLENGES_KEY).data(new Data(challenges)).start().awaitUninterruptibly();
            reloadPlayers();
            notifyAllPlayersAboutChallengeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean reloadChallenge(String gameCode) throws Exception {
        try {
            FutureGet futureGet = dht.get(Number160.createHash(gameCode)).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (futureGet.isEmpty()) {
                    currentChallenge.setTerminated(true);
                } else {
                    currentChallenge = (Challenge) futureGet.dataMap().values().iterator().next().object();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean sendUpdatedChallenge() throws Exception {
        try {
            for (Map.Entry<String, Integer> entry : currentChallenge.getPlayers_scores().entrySet()) {
                int playerIndex = findPlayerIndex(entry.getKey());
                
                if (playerIndex != -1) {
                    FutureDirect futureDirect = dht.peer()
                            .sendDirect(players.get(playerIndex).getPeerAdd())
                            .object(currentChallenge)
                            .start();
                    futureDirect.awaitUninterruptibly();
                }
            }
            return true;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Player non trovato nella lista dei partecipanti");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean startChallenge(String gameCode) throws Exception {
        try {
            FutureGet futureGet = dht.get(Number160.createHash(gameCode)).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (futureGet.isEmpty()) {
                    currentChallenge.setTerminated(true);
                } else {
                    currentChallenge = (Challenge) futureGet.dataMap().values().iterator().next().object();
                    currentChallenge.setStarted(true);
                    
                    dht.put(Number160.createHash(gameCode))
                            .data(new Data(currentChallenge))
                            .start()
                            .awaitUninterruptibly();

                    notifyAllChallengeParticipants();
                }
                return true;
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Player non trovato nella lista dei partecipanti");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Notifies all challenge participants about updates
     */
    private void notifyAllChallengeParticipants() throws Exception {
        for (Map.Entry<String, Integer> entry : currentChallenge.getPlayers_scores().entrySet()) {
            if (entry.getKey().equals(currentPlayer.getNickname())) {
                continue;
            }
            
            int playerIndex = findPlayerIndex(entry.getKey());
            if (playerIndex != -1) {
                FutureDirect futureDirect = dht.peer()
                        .sendDirect(players.get(playerIndex).getPeerAdd())
                        .object(currentChallenge)
                        .start();
                futureDirect.awaitUninterruptibly();
            }
        }
    }

    @Override
    public boolean joinChallenge(String gameCode) throws Exception {
        try {
            FutureGet futureGet = dht.get(Number160.createHash(gameCode)).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (futureGet.isEmpty()) {
                    throw new ChallengeNotFoundException();
                }

                currentChallenge = (Challenge) futureGet.dataMap().values().iterator().next().object();
                currentChallenge.getPlayers_scores().put(currentPlayer.getNickname(), 0);
                
                dht.put(Number160.createHash(gameCode))
                        .data(new Data(currentChallenge))
                        .start()
                        .awaitUninterruptibly();

                updateChallengeList();
                sendUpdatedChallenge();
            }
            return true;
        } catch (ChallengeNotFoundException e) {
            System.out.println("Partita non trovata nella lista delle partite disponibili.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean quitChallenge(String gameCode) throws Exception {
        try {
            FutureGet futureGet = dht.get(Number160.createHash(gameCode)).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (futureGet.isEmpty()) {
                    currentChallenge.setTerminated(true);
                    return true;
                }

                currentChallenge = (Challenge) futureGet.dataMap().values().iterator().next().object();
                currentChallenge.getPlayers_scores().remove(currentPlayer.getNickname());

                if (currentChallenge.getPlayers_scores().size() == 0) {
                    removeChallenge(currentChallenge.getCodice_partita());
                } else {
                    handlePlayerQuit(gameCode);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Handles the logic when a player quits the challenge
     */
    private void handlePlayerQuit(String gameCode) throws Exception {
        if (currentChallenge.getPlayers_scores().size() != 1) {
            updateChallengeList();
        } else {
            removeFromChallengeList();
        }

        notifyAllChallengeParticipants();
        
        dht.put(Number160.createHash(gameCode))
                .data(new Data(currentChallenge))
                .start()
                .awaitUninterruptibly();
    }

    @Override
    public Integer placeNumber(String gameCode, int x, int y, int value) throws Exception {
        try {
            FutureGet futureGet = dht.get(Number160.createHash(gameCode)).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (futureGet.isEmpty()) {
                    currentChallenge.setTerminated(true);
                    return -100;
                }

                currentChallenge = (Challenge) futureGet.dataMap().values().iterator().next().object();
                
                Integer result = processNumberPlacement(x, y, value);
                
                if (isSudokuComplete()) {
                    handleSudokuCompletion();
                }

                dht.put(Number160.createHash(gameCode))
                        .data(new Data(currentChallenge))
                        .start()
                        .awaitUninterruptibly();
                
                sendUpdatedChallenge();
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -100;
    }

    /**
     * Processes the number placement and returns the score change
     */
    private Integer processNumberPlacement(int x, int y, int value) {
        String playerNickname = currentPlayer.getNickname();
        int currentScore = currentChallenge.getPlayers_scores().get(playerNickname);
        
        int correctValue = currentChallenge.getSudoku_board().getSudoku_risolto()[x][y];
        int currentCellValue = currentChallenge.getSudoku_board().getSudoku_sfida()[x][y];

        if (correctValue != value) {
            // Wrong value - lose a point
            currentChallenge.getPlayers_scores().put(playerNickname, currentScore - 1);
            return -1;
        } else if (currentCellValue == 0) {
            // Correct value in empty cell - gain a point
            currentChallenge.getPlayers_scores().put(playerNickname, currentScore + 1);
            currentChallenge.getSudoku_board().getSudoku_sfida()[x][y] = value;
            return 1;
        } else {
            // Correct value but cell already filled - no points
            return 0;
        }
    }

    /**
     * Checks if the Sudoku is complete
     */
    private boolean isSudokuComplete() {
        return currentChallenge.getSudoku_board()
                .contaZeri(currentChallenge.getSudoku_board().getSudoku_sfida()) == 0;
    }

    /**
     * Handles the completion of the Sudoku game
     */
    private void handleSudokuCompletion() throws Exception {
        currentChallenge.setFull(true);
        
        Map.Entry<String, Integer> winner = findWinner();
        currentChallenge.setTerminated(true);
        currentChallenge.setWinner(new Pair<>(winner.getKey(), winner.getValue()));
        
        challenges.remove(findCurrentChallengeIndex());
        currentChallenge.getSudoku_board()
                .printSudoku(currentChallenge.getSudoku_board().getSudoku_sfida());
        
        removeFromChallengeList();
    }

    /**
     * Finds the winner with the highest score
     */
    private Map.Entry<String, Integer> findWinner() {
        return currentChallenge.getPlayers_scores()
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    /**
     * Finds the index of the current challenge in the challenges list
     */
    private int findCurrentChallengeIndex() {
        for (int i = 0; i < challenges.size(); i++) {
            if (challenges.get(i).getCodice_partita().equals(currentChallenge.getCodice_partita())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the index of a player by nickname
     */
    private int findPlayerIndex(String nickname) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getNickname().equals(nickname)) {
                return i;
            }
        }
        return -1;
    }

    public boolean leaveNetwork() {
        try {
            FutureGet futureGet = dht.get(PLAYERS_KEY).start().awaitUninterruptibly();

            if (futureGet.isSuccess()) {
                if (!futureGet.isEmpty()) {
                    players = (ArrayList<Player>) futureGet.dataMap().values().iterator().next().object();
                    
                    if (players.size() == 1) {
                        players.clear();
                    } else {
                        players.remove(findPlayerIndex(currentPlayer.getNickname()));
                    }
                    
                    dht.put(PLAYERS_KEY).data(new Data(players)).start().awaitUninterruptibly();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void shutdown() {
        dht.peer().announceShutdown().start().awaitUninterruptibly();

        challenges.clear();
        players.clear();
        currentChallenge = null;
        currentPlayer = null;
        peer.shutdown();
    }

    // Getters and Setters
    public ArrayList<Challenge> getChallenges() {
        return challenges;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public void setPlayers(ArrayList<Player> players) {
        this.players = players;
    }

    public void setChallenges(ArrayList<Challenge> challenges) {
        this.challenges = challenges;
    }

    public void setChallenge(Challenge challenge) {
        this.currentChallenge = challenge;
    }

    public Challenge getChallenge() {
        return currentChallenge;
    }

    public Player getPlayer() {
        return currentPlayer;
    }
}