package com.github.emusto3.game;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.beryx.textio.StringInputReader;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;

import com.github.emusto3.beans.Challenge;
import com.github.emusto3.beans.Pair;
import com.github.emusto3.beans.Player;
import com.github.emusto3.beans.Sudoku;
import com.github.emusto3.exceptions.MasterPeerNotFoundException;
import com.github.emusto3.interfaces.Client;
import com.github.emusto3.interfaces.MessageListener;

import java.awt.Robot;
import java.awt.event.KeyEvent;

/**
 * Main class for the Sudoku multiplayer game
 * Handles UI interactions and game flow
 */
public class SudokuGame {

    // Game Configuration Constants
    private static final int MIN_NICKNAME_LENGTH = 3;
    private static final int MAX_NICKNAME_LENGTH = 7;
    private static final int GAME_EXIT_COUNTDOWN = 6;
    private static final int SUDOKU_SIZE = 9;
    private static final int TERMINAL_WIDTH = 700;
    private static final int TERMINAL_HEIGHT = 700;
    
    // Command Constants
    private static final String REFRESH_COMMAND = "refresh";
    private static final String EXIT_COMMAND = "exit";
    private static final String CREATE_CHALLENGE_PREFIX = "@";
    private static final String JOIN_CHALLENGE_PREFIX = ">";
    private static final String REFRESH_MARKER = " refresh! ";
    
    // UI Color Constants
    private static final String COLOR_WHITE = "white";
    private static final String COLOR_RED = "red";
    private static final String COLOR_GREEN = "green";
    private static final String COLOR_YELLOW = "yellow";
    
    // UI Components
    private static final TextIO textIO = TextIoFactory.getTextIO();
    private static final TextTerminal terminal = textIO.getTextTerminal();
    private static Client peer;

    // Command line options
    @Option(name = "-m", aliases = "--masterip", usage = "the master peer ip address", required = true)
    private static String masterPeerIP;

    @Option(name = "-id", aliases = "--identifierpeer", usage = "the unique identifier for this peer", required = true)
    private static int peerID;

    /**
     * Constructor - Parses command line arguments and initializes the game
     */
    public SudokuGame(String[] args) throws Exception {
        clearScreen();
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(args);
    }

    /**
     * Main entry point for the application
     */
    public static void main(String[] args) throws Exception {
        SudokuGame game = new SudokuGame(args);
        
        try {
            initializePeer();
            game.runGameLoop();
        } catch (Exception e) {
            handleMainException(e);
        }
    }

    /**
     * Initializes the P2P peer connection
     */
    private static void initializePeer() throws Exception {
        MessageListenerImpl messageListener = new MessageListenerImpl();
        
        try {
            peer = new ClientImpl(masterPeerIP, peerID, messageListener);
        } catch (MasterPeerNotFoundException e) {
            System.out.println("Master peer non trovato.");
            Thread.sleep(3000);
            System.exit(1);
        }
    }

    /**
     * Handles exceptions in the main method
     */
    private static void handleMainException(Exception e) {
        e.printStackTrace();
    }

    /**
     * Main game loop - shows home screen then choices screen
     */
    private void runGameLoop() throws Exception {
        showHomeScreen();
        showChoicesScreen();
    }

    /**
     * Custom message listener implementation for handling P2P messages
     */
    private static class MessageListenerImpl implements MessageListener {

        @Override
        public Object parseMessage(Object obj) throws Exception {
            if (isChallengeListUpdate(obj)) {
                handleChallengeListUpdate(obj);
            } else if (isChallengeUpdate(obj)) {
                handleChallengeUpdate(obj);
            }
            return "success";
        }

        /**
         * Checks if the received object is a challenge list update
         */
        private boolean isChallengeListUpdate(Object obj) {
            return obj.getClass().equals(peer.getChallenges().getClass());
        }

        /**
         * Checks if the received object is a challenge update
         */
        private boolean isChallengeUpdate(Object obj) {
            return peer.getChallenge() != null && 
                   obj.getClass().equals(peer.getChallenge().getClass());
        }

        /**
         * Handles updates to the challenge list
         */
        private void handleChallengeListUpdate(Object obj) throws Exception {
            peer.setChallenges((ArrayList<Challenge>) obj);
            if (peer.getChallenge() == null) {
                showUpdateNotification("Nuove partite create, clicca invio per aggiornare");
            }
        }

        /**
         * Handles updates to the current challenge
         */
        private void handleChallengeUpdate(Object obj) throws Exception {
            peer.setChallenge((Challenge) obj);
            if (peer.getChallenge() != null) {
                showUpdateNotification("Aggiornamento sfida, clicca invio per aggiornare");
            }
        }

        /**
         * Shows an update notification to the user
         */
        private void showUpdateNotification(String message) throws Exception {
            terminal.resetLine();
            setTerminalColor(COLOR_YELLOW);
            terminal.println("\n\n!!! " + message + " !!!");
            setTerminalColor(COLOR_WHITE);
            Thread.sleep(1000);
        }
    }

    /**
     * Displays the home screen and handles user login
     */
    public void showHomeScreen() throws Exception {
        initializeTerminal();

        while (true) {
            terminal.setBookmark("BOOKMARK");
            terminal.println(" ");
            displayGameLogo();

            String nickname = promptForNickname();

            if (isValidNickname(nickname) && registerPlayer(nickname)) {
                terminal.resetToBookmark("BOOKMARK");
                break;
            } else {
                displayNicknameError(nickname);
                terminal.resetToBookmark("BOOKMARK");
            }
        }
    }

    /**
     * Initializes terminal settings
     */
    private void initializeTerminal() {
        terminal.getProperties().setPromptColor(COLOR_WHITE);
        terminal.getProperties().setPaneDimension(TERMINAL_WIDTH, TERMINAL_HEIGHT);
    }

    /**
     * Prompts user for nickname input
     */
    private String promptForNickname() {
        return textIO.newStringInputReader()
                .withDefaultValue(" ")
                .read("   Inserisci un nickname");
    }

    /**
     * Validates nickname length and format
     */
    private boolean isValidNickname(String nickname) {
        return nickname.length() >= MIN_NICKNAME_LENGTH && 
               nickname.length() <= MAX_NICKNAME_LENGTH;
    }

    /**
     * Attempts to register the player with the given nickname
     */
    private boolean registerPlayer(String nickname) throws Exception {
        return peer.checkPlayer(nickname);
    }

    /**
     * Displays appropriate error message for invalid nickname
     */
    private void displayNicknameError(String nickname) throws Exception {
        terminal.println("  ");
        terminal.println("  ");
        setTerminalColor(COLOR_RED);
        
        String errorMessage = !isValidNickname(nickname) 
            ? "La lunghezza del nickname deve essere compresa tra " + 
              MIN_NICKNAME_LENGTH + " e " + MAX_NICKNAME_LENGTH + " caratteri"
            : "Nickname già utilizzato";
            
        terminal.println("  " + errorMessage);
        Thread.sleep(2000);
        setTerminalColor(COLOR_WHITE);
    }

    /**
     * Displays the choices screen with available challenges
     */
    public void showChoicesScreen() throws Exception {
        clearScreen();

        while (true) {
            terminal.setBookmark("TABELLONE");
            peer.reloadChallengeList();
            clearScreen();
            
            displayChallengeBoard();
            String userInput = promptForChallengeAction();
            
            if (isRefreshCommand(userInput)) {
                Thread.sleep(300);
                continue;
            }
            
            terminal.resetToBookmark("TABELLONE");
            
            if (handleUserCommand(userInput)) {
                break; // Exit game
            }
            
            terminal.resetToBookmark("BOOKMARK");
        }
    }

    /**
     * Displays the challenge board header and available challenges
     */
    private void displayChallengeBoard() {
        displayPeerInfo();
        displayChallengeTableHeader();
        displayAvailableChallenges();
    }

    /**
     * Displays peer information
     */
    private void displayPeerInfo() {
        terminal.println("[PEER " + peerID + " | " + peer.getPlayer().getNickname() + "]");
    }

    /**
     * Displays the challenge table header
     */
    private void displayChallengeTableHeader() {
        terminal.println("\n\n-------------------------------------------------------------------");
        terminal.println(" Codice partita             N. Giocatori           Creatore stanza");
        terminal.println("-------------------------------------------------------------------");
    }

    /**
     * Displays available challenges or no challenges message
     */
    private void displayAvailableChallenges() {
        if (hasChallenges()) {
            displayChallengesList();
        } else {
            displayNoChallengesMessage();
        }
    }

    /**
     * Checks if there are any challenges available
     */
    private boolean hasChallenges() {
        return peer.getChallenges() != null && 
               !peer.getChallenges().isEmpty();
    }

    /**
     * Displays the list of available challenges
     */
    private void displayChallengesList() {
        for (Challenge challenge : peer.getChallenges()) {
            String formattedGameCode = formatGameCodeForDisplay(challenge.getCodice_partita());
            int playerCount = challenge.getPlayers_scores().size();
            String creator = challenge.getOwner();
            
            terminal.println("\n  " + formattedGameCode + "\t\t\t" + playerCount + "\t\t\t" + creator);
        }
    }

    /**
     * Formats the game code for proper display alignment
     */
    private String formatGameCodeForDisplay(String gameCode) {
        return String.format("%-7s", gameCode);
    }

    /**
     * Displays message when no challenges are available
     */
    private void displayNoChallengesMessage() {
        terminal.print("\n\n\t\t\tNon ci sono sfide attive...\n\n\n");
    }

    /**
     * Prompts user for challenge board action
     */
    private String promptForChallengeAction() {
        return textIO.newStringInputReader()
                .withDefaultValue(REFRESH_COMMAND)
                .read(buildChallengePrompt());
    }

    /**
     * Builds the challenge board prompt text
     */
    private String buildChallengePrompt() {
        return "\n\n\n   TABELLONE SFIDE\n\n" +
               "  Digita '>' e un codice partita per partecipare ad una partita.\n" +
               "  Digita '@' e un nuovo codice partita per creare una nuova sfida.\n" +
               "  Digita 'exit' per uscire dal gioco.\n\n" +
               "  >   ";
    }

    /**
     * Checks if the input is a refresh command
     */
    private boolean isRefreshCommand(String userInput) {
        return userInput.contains(REFRESH_MARKER);
    }

    /**
     * Handles user commands from the challenge board
     * @return true if user wants to exit, false otherwise
     */
    private boolean handleUserCommand(String userInput) throws Exception {
        if (userInput.isEmpty()) {
            return false;
        }

        char commandPrefix = userInput.charAt(0);
        
        switch (commandPrefix) {
            case '@':
                return handleCreateChallengeCommand(userInput);
            case '>':
                return handleJoinChallengeCommand(userInput);
            default:
                return handleOtherCommands(userInput);
        }
    }

    /**
     * Handles challenge creation command
     */
    private boolean handleCreateChallengeCommand(String userInput) throws Exception {
        String gameCode = extractGameCodeFromInput(userInput);
        
        if (isInvalidGameCode(gameCode)) {
            displayError("Codice partita non valido");
            return false;
        }

        if (peer.generateNewSudoku(gameCode, 0)) {
            showGameScreen();
        } else {
            displayError("Codice partita già presente");
        }
        return false;
    }

    /**
     * Handles challenge joining command
     */
    private boolean handleJoinChallengeCommand(String userInput) throws Exception {
        String gameCode = extractGameCodeFromInput(userInput);

        if (isInvalidGameCode(gameCode)) {
            displayError("Codice partita non valido");
            return false;
        }

        if (peer.joinChallenge(gameCode)) {
            showGameScreen();
        } else {
            displayError("Codice partita non presente");
        }
        return false;
    }

    /**
     * Extracts game code from user input (removes prefix)
     */
    private String extractGameCodeFromInput(String userInput) {
        return userInput.substring(1);
    }

    /**
     * Handles other commands (like exit)
     */
    private boolean handleOtherCommands(String userInput) throws Exception {
        if (EXIT_COMMAND.equals(userInput)) {
            exitGame();
            return true;
        }
        return false;
    }

    /**
     * Checks if a game code is invalid (contains spaces or is empty)
     */
    private boolean isInvalidGameCode(String gameCode) {
        return gameCode.contains(" ") || gameCode.isEmpty();
    }

    /**
     * Main game screen where the Sudoku challenge takes place
     */
    public void showGameScreen() throws Exception {
        int countdown = GAME_EXIT_COUNTDOWN;

        while (true) {
            refreshGameScreen();
            
            if (!peer.getChallenge().isTerminated()) {
                peer.reloadChallenge(peer.getChallenge().getCodice_partita());
            }

            displayGameInterface();

            GameStateResult stateResult = handleGameState();
            if (stateResult.shouldExit) {
                return;
            }

            if (peer.getChallenge().isTerminated()) {
                countdown = handleGameTermination(countdown);
                if (countdown <= 0) {
                    return;
                }
            } else {
                if (handleGameInput()) {
                    return; // Player quit
                }
            }

            terminal.resetToBookmark("BOOKMARK");
        }
    }

    /**
     * Refreshes the game screen display
     */
    private void refreshGameScreen() {
        clearScreen();
        terminal.resetToBookmark("BOOKMARK");
    }

    /**
     * Displays the complete game interface
     */
    private void displayGameInterface() throws Exception {
        displayGameHeader();
        displaySudokuBoard();
        displayGameInstructions();
    }

    /**
     * Displays the game header with peer info and challenge code
     */
    private void displayGameHeader() {
        terminal.println("[PEER " + peerID + " | " + peer.getPlayer().getNickname() + "]");
        terminal.println("\n\t  SUDOKU GAME - " + peer.getChallenge().getCodice_partita());
        terminal.println("\n");
    }

    /**
     * Displays the Sudoku board with player scores
     */
    private void displaySudokuBoard() throws Exception {
        ArrayList<Pair<String, Integer>> playerScores = extractPlayerScores();
        char[] rowLabels = createRowLabels();
        
        displayBoardHeader();
        displayBoardRows(playerScores, rowLabels);
    }

    /**
     * Extracts player scores from the current challenge
     */
    private ArrayList<Pair<String, Integer>> extractPlayerScores() {
        ArrayList<Pair<String, Integer>> scores = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : peer.getChallenge().getPlayers_scores().entrySet()) {
            scores.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        
        return scores;
    }

    /**
     * Creates row labels for the Sudoku board
     */
    private char[] createRowLabels() {
        return new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
    }

    /**
     * Displays the board header
     */
    private void displayBoardHeader() {
        terminal.println("     A   B   C   D   E   F   G   H   I");
        terminal.println("   +---+---+---+---+---+---+---+---+---+" + "\t Nickname  Punteggio");
    }

    /**
     * Displays all board rows with scores
     */
    private void displayBoardRows(ArrayList<Pair<String, Integer>> scores, char[] rowLabels) {
        Sudoku sudoku = peer.getChallenge().getSudoku_board();
        
        for (int i = 0; i < SUDOKU_SIZE; i++) {
            String scoreInfo = getScoreInfoForRow(scores, i);
            String rowData = buildSudokuRowDisplay(sudoku, i, rowLabels[i]);
            
            terminal.println(rowData + scoreInfo);
            terminal.println("   +---+---+---+---+---+---+---+---+---+");
        }
    }

    /**
     * Gets score information for a specific row (for display alignment)
     */
    private String getScoreInfoForRow(ArrayList<Pair<String, Integer>> scores, int rowIndex) {
        if (shouldDisplayScoreForRow(scores, rowIndex)) {
            Pair<String, Integer> playerScore = scores.get(rowIndex - 1);
            String formattedPlayerName = String.format("%-7s", playerScore.element0());
            return "\t " + formattedPlayerName + "       " + playerScore.element1();
        }
        return "";
    }

    /**
     * Determines if score should be displayed for a given row
     */
    private boolean shouldDisplayScoreForRow(ArrayList<Pair<String, Integer>> scores, int rowIndex) {
        return scores.size() > (rowIndex - 1) && rowIndex != 0;
    }

    /**
     * Builds the display string for a single Sudoku row
     */
    private String buildSudokuRowDisplay(Sudoku sudoku, int row, char rowLabel) {
        StringBuilder rowBuilder = new StringBuilder();
        rowBuilder.append(" ").append(rowLabel).append(" ");
        
        int[][] board = sudoku.getSudoku_sfida();
        
        for (int col = 0; col < SUDOKU_SIZE; col++) {
            String cellValue = (board[row][col] == 0) ? " " : String.valueOf(board[row][col]);
            rowBuilder.append("| ").append(cellValue).append(" ");
        }
        
        rowBuilder.append("|");
        return rowBuilder.toString();
    }

    /**
     * Displays game instructions
     */
    private void displayGameInstructions() {
        terminal.println("\n\n\n Digita 'XY-N' per inserire il valore N nella cella X,Y");
        terminal.println(" Digita 'exit' per abbandonare la partita");
    }

    /**
     * Inner class to represent game state handling results
     */
    private static class GameStateResult {
        final boolean shouldExit;
        
        GameStateResult(boolean shouldExit) {
            this.shouldExit = shouldExit;
        }
    }

    /**
     * Handles various game states (waiting, started, terminated)
     */
    private GameStateResult handleGameState() throws Exception {
        Challenge currentChallenge = peer.getChallenge();
        
        if (isWaitingForPlayers(currentChallenge)) {
            displayWaitingMessage();
            return new GameStateResult(false);
        }
        
        if (shouldStartChallenge(currentChallenge)) {
            peer.startChallenge(currentChallenge.getCodice_partita());
        }
        
        if (shouldTerminateForSinglePlayer(currentChallenge)) {
            terminal.resetToBookmark("BOOKMARK");
            currentChallenge.setTerminated(true);
        }
        
        return new GameStateResult(false);
    }

    /**
     * Checks if the game is waiting for more players
     */
    private boolean isWaitingForPlayers(Challenge challenge) {
        return !challenge.isStarted() && 
               challenge.getPlayers_scores().size() < 2 && 
               !challenge.isTerminated();
    }

    /**
     * Checks if the challenge should be started
     */
    private boolean shouldStartChallenge(Challenge challenge) {
        return !challenge.isStarted() && challenge.getPlayers_scores().size() > 1;
    }

    /**
     * Checks if the challenge should be terminated due to single player
     */
    private boolean shouldTerminateForSinglePlayer(Challenge challenge) {
        return challenge.isStarted() && 
               challenge.getPlayers_scores().size() == 1 && 
               !challenge.isTerminated();
    }

    /**
     * Displays waiting message for game start
     */
    private void displayWaitingMessage() throws Exception {
        setTerminalColor(COLOR_YELLOW);
        terminal.getProperties().setPromptItalic(true);
        terminal.println(" \nLa partita comincerà dall'ingresso del secondo giocatore");
        Thread.sleep(1500);
        setTerminalColor(COLOR_WHITE);
        terminal.getProperties().setPromptItalic(false);
    }

    /**
     * Handles game termination display and countdown
     * @return updated countdown value
     */
    private int handleGameTermination(int countdown) throws Exception {
        displayGameTerminationMessage();
        
        Challenge currentChallenge = peer.getChallenge();
        
        if (currentChallenge.isFull()) {
            displayWinnerInfo(currentChallenge);
        }

        if (isSinglePlayerRemaining(currentChallenge)) {
            peer.quitChallenge(currentChallenge.getCodice_partita());
            resetTerminalFormatting();
            Thread.sleep(1000);
            return 0; // Signal to exit
        }

        return displayCountdownAndDecrement(countdown);
    }

    /**
     * Displays game termination message
     */
    private void displayGameTerminationMessage() {
        setTerminalColor(COLOR_GREEN);
        terminal.getProperties().setPromptBold(true);
        terminal.println(" La partita è terminata. ");
    }

    /**
     * Displays winner information
     */
    private void displayWinnerInfo(Challenge challenge) {
        Pair<String, Integer> winner = challenge.getWinner();
        terminal.println("  \n" + winner.element0() + " vince con punti pari a " + winner.element1());
    }

    /**
     * Checks if only one player remains
     */
    private boolean isSinglePlayerRemaining(Challenge challenge) {
        return challenge.getPlayers_scores().size() == 1;
    }

    /**
     * Displays countdown and decrements it
     */
    private int displayCountdownAndDecrement(int countdown) throws Exception {
        resetTerminalFormatting();
        terminal.println("\n Sarai reindirizzato al tabellone sfide tra " + countdown + " secondi...");
        Thread.sleep(1000);
        return countdown - 1;
    }

    /**
     * Handles user input during the game
     * @return true if player quit
     */
    private boolean handleGameInput() throws Exception {
        String userInput = promptForGameInput();
        
        if (EXIT_COMMAND.equals(userInput)) {
            peer.quitChallenge(peer.getChallenge().getCodice_partita());
            return true;
        }

        if (shouldIgnoreInput()) {
            return false;
        }

        if (isValidGameInput(userInput.toUpperCase())) {
            handleNumberPlacement(userInput.toUpperCase());
        }

        return false;
    }

    /**
     * Prompts user for game input
     */
    private String promptForGameInput() {
        return textIO.newStringInputReader()
                .withDefaultValue(REFRESH_COMMAND)
                .read("\n   > ");
    }

    /**
     * Determines if input should be ignored based on game state
     */
    private boolean shouldIgnoreInput() {
        Challenge currentChallenge = peer.getChallenge();
        return !currentChallenge.isStarted() && 
               currentChallenge.getPlayers_scores().size() < 2 && 
               !currentChallenge.isTerminated();
    }

    /**
     * Handles number placement in the Sudoku board
     */
    private void handleNumberPlacement(String input) throws Exception {
        SudokuMove move = parseSudokuMove(input);
        Integer result = peer.placeNumber(
            peer.getChallenge().getCodice_partita(), 
            move.x, 
            move.y, 
            move.value
        );
        
        displayPlacementResult(result);
    }

    /**
     * Inner class to represent a Sudoku move
     */
    private static class SudokuMove {
        final int x;
        final int y;
        final int value;
        
        SudokuMove(int x, int y, int value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }
    }

    /**
     * Parses a Sudoku move from user input
     */
    private SudokuMove parseSudokuMove(String input) {
        int x = input.charAt(0) - 'A';
        int y = input.charAt(1) - 'A';
        int value = input.charAt(3) - '0';
        return new SudokuMove(x, y, value);
    }

    /**
     * Displays the result of number placement
     */
    private void displayPlacementResult(Integer result) throws Exception {
        PlacementResult placementResult = interpretPlacementResult(result);
        
        setTerminalColor(placementResult.color);
        terminal.println(placementResult.message);
        Thread.sleep(1000);
        setTerminalColor(COLOR_WHITE);
        terminal.resetToBookmark("BOOKMARK");
    }

    /**
     * Inner class to represent placement result information
     */
    private static class PlacementResult {
        final String message;
        final String color;
        
        PlacementResult(String message, String color) {
            this.message = message;
            this.color = color;
        }
    }

    /**
     * Interprets the placement result code
     */
    private PlacementResult interpretPlacementResult(Integer result) {
        switch (result) {
            case 1:
                return new PlacementResult("Valore corretto!", COLOR_GREEN);
            case -1:
                return new PlacementResult("Valore errato!", COLOR_RED);
            case 0:
                return new PlacementResult("Valore già presente!", COLOR_YELLOW);
            default:
                return new PlacementResult("Valore non inviato!", COLOR_WHITE);
        }
    }

    /**
     * Displays the ASCII art game logo
     */
    private void displayGameLogo() {
        String[] logoLines = {
            "   .--.        .-.    .-.           .--.                      ",
            "  : .--'       : :    : :.-.       : .--'                     ",
            "  `. `..-..-..-' :.--.: `'..-..-.  : : _ .--. ,-.,-.,-..--.   ",
            "   _`, : :; ' .; ' .; : . `: :; :  : :; ' .; ;: ,. ,. ' '_.'  ",
            "  `.__.`.__.`.__.`.__.:_;:_`.__.'  `.__.`.__,_:_;:_;:_`.__.'  ",
            "                                                              ",
            "                                                              "
        };
        
        for (String line : logoLines) {
            terminal.println(line);
        }
    }

    /**
     * Utility method to set terminal text color
     */
    private static void setTerminalColor(String color) {
        terminal.getProperties().setPromptColor(color);
    }

    /**
     * Resets terminal formatting to default
     */
    private void resetTerminalFormatting() {
        terminal.getProperties().setPromptBold(false);
        setTerminalColor(COLOR_WHITE);
    }

    /**
     * Clears the terminal screen
     */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Displays a generic error message
     */
    private void displayError(String errorMessage) throws Exception {
        terminal.println("  ");
        terminal.println("  ");
        setTerminalColor(COLOR_RED);
        terminal.println("  " + errorMessage);
        Thread.sleep(1000);
        setTerminalColor(COLOR_WHITE);
        terminal.resetToBookmark("BOOKMARK");
    }

    /**
     * Exits the game gracefully
     */
    public void exitGame() throws Exception {
        try {
            peer.leaveNetwork();
            peer.shutdown();
            terminal.dispose();
            Thread.sleep(300);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validates game input format (e.g., "A1-5")
     * Input must be: [A-I][A-I]-[1-9]
     */
    public boolean isValidGameInput(String input) {
        if (input.length() != 4) {
            return false;
        }

        return isValidCoordinate(input.charAt(0)) &&
               isValidCoordinate(input.charAt(1)) &&
               input.charAt(2) == '-' &&
               isValidNumber(input.charAt(3));
    }

    /**
     * Validates if a character is a valid coordinate (A-I)
     */
    private boolean isValidCoordinate(char coordinate) {
        return coordinate >= 'A' && coordinate <= 'I';
    }

    /**
     * Validates if a character is a valid Sudoku number (1-9)
     */
    private boolean isValidNumber(char number) {
        return number >= '1' && number <= '9';
    }
}