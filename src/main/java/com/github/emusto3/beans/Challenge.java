package com.github.emusto3.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Challenge implements Serializable{
	
	private HashMap<String, Integer> players_scores = new HashMap<String, Integer>();
	private String owner;
	private String codice_partita;
	private Pair<String, Integer> winner;
	private Sudoku sudoku_board;
	private boolean isTerminated = false;
	private boolean isStarted = false;
	private boolean isFull = false;
	
	public boolean isFull() {
		return isFull;
	}




	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}




	public boolean isStarted() {
		return isStarted;
	}




	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}









	public Pair<String, Integer> getWinner() {
		return winner;
	}




	public void setWinner(Pair<String, Integer> winner) {
		this.winner = winner;
	}




	public Challenge(String codice_partita, String nickname, int seed) {
		
		this.codice_partita = codice_partita;
		this.owner = nickname;
		this.players_scores.put(nickname, 0);
		this.sudoku_board = new Sudoku(seed == -1 ? 0 : new Random().nextInt(8));
		
	}
	


	
	



	public HashMap<String, Integer> getPlayers_scores() {
		return players_scores;
	}




	public void setPlayers_scores(HashMap<String, Integer> players_scores) {
		this.players_scores = players_scores;
	}




	public String getOwner() {
		return owner;
	}




	public void setOwner(String owner) {
		this.owner = owner;
	}




	public boolean isTerminated() {
		return isTerminated;
	}




	public void setTerminated(boolean isTerminated) {
		this.isTerminated = isTerminated;
	}







	public String getCodice_partita() {
		return codice_partita;
	}

	public void setCodice_partita(String codice_partita) {
		this.codice_partita = codice_partita;
	}

	public Sudoku getSudoku_board() {
		return sudoku_board;
	}

	public void setSudoku_board(Sudoku sudoku_board) {
		this.sudoku_board = sudoku_board;
	}




	
	

}
