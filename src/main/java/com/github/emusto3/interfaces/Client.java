package com.github.lorenzopetra96.interfaces;

import java.util.ArrayList;

import com.github.lorenzopetra96.beans.Challenge;
import com.github.lorenzopetra96.beans.Pair;
import com.github.lorenzopetra96.beans.Player;

public interface Client {

	// Controllo del nickname del giocatore durante il login
	public boolean checkPlayer(String nickname) throws Exception;
	
	// Creazione nuovo sudoku
	public boolean generateNewSudoku(String codice_partita, int seed) throws Exception;
	
	// Controllo su codice partita da creare se già esistente 
	public boolean checkChallenge(String codice_partita) throws Exception;
	
	// Partecipazione ad una partita
	public boolean joinChallenge(String codice_partita) throws Exception;
	
	// Aggiornamento passivo lista delle partite disponibili (Ricezione lista partite aggiornata)
	public void reloadChallengeList() throws Exception;
	
	// Inserimento partita nella lista delle sfide disponibili
	public boolean addChallenge() throws Exception;
	
	// Eliminazione partita 
	public boolean removeChallenge(String codice_partita) throws Exception;
	
	// Rimozione partita dalla lista delle partite disponibili
	public void removeFromChallengeList() throws Exception;
	
	// Aggiornamento attivo lista delle partite disponibili (Invio lista partite modificata)
	public void updateChallengeList() throws Exception;
	
	// Aggiornamento passivo singola partita
	public boolean reloadChallenge(String codice_partita) throws Exception;
	
	// Avvio effettivo della partita
	public boolean startChallenge(String codice_partita) throws Exception;
	

	// Abbandono partita
	public boolean quitChallenge(String codice_partita) throws Exception;
	
	// Trova indice partita in corso nella lista di partite disponibili
	public int findChallenge();
	
	// Inserimento valore nella sudoku board
	public Integer placeNumber(String codice_partita, int x, int y, int value) throws Exception;
	
	// Abbandono rete
	public boolean leaveNetwork();
	
	// Spegnimento peer
	public void shutdown();
	
	// Recupero lista partite disponibili
	public ArrayList<Challenge> getChallenges();

	// Recupero partita
	public Challenge getChallenge();

	// Recupero informazioni player
	public Player getPlayer();
	
	public ArrayList<Player> getPlayers();
	
	
	public void setPlayers(ArrayList<Player> players);
	
	public void setChallenges(ArrayList<Challenge> challenges);

	public void setChallenge(Challenge challenge);
	
	public void reloadPlayers() throws Exception;
	
	public boolean sendUpdatedChallenge() throws Exception;

	
}
