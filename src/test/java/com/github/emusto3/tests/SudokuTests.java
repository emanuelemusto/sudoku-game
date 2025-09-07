package com.github.lorenzopetra96.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.beryx.textio.TextTerminal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.lorenzopetra96.beans.Challenge;
import com.github.lorenzopetra96.exceptions.MasterPeerNotFoundException;
import com.github.lorenzopetra96.game.ClientImpl;
import com.github.lorenzopetra96.game.SudokuGame;
import com.github.lorenzopetra96.interfaces.Client;
import com.github.lorenzopetra96.interfaces.MessageListener;


public class SudokuTests {

	
	protected Client peer1,	peer2, peer3, peer4;
	
	public SudokuTests() throws Exception{
		
		
		
	}
			
	class MessageListenerImpl implements MessageListener{
		Client peer;
		public MessageListenerImpl(){}
		public MessageListenerImpl(Client peer){this.peer = peer;}
		public Object parseMessage(Object obj) throws Exception {
		if(peer!=null)
			try{
				System.out.println(obj.getClass());

				if(obj.getClass().equals(peer.getChallenges().getClass())) {
					peer.setChallenges((ArrayList<Challenge>) obj);
				}
				else if(obj.getClass().equals(peer.getChallenge().getClass())) {
					peer.setChallenge((Challenge) obj);
				}
				return "success";
			}catch(Exception e) {
				e.printStackTrace();
			}
			return "success";
		}


	}

	@BeforeEach
	public void init() throws Exception{
		peer1 = new ClientImpl("127.0.0.1", 0, new MessageListenerImpl(peer1));
		peer2 = new ClientImpl("127.0.0.1", 1, new MessageListenerImpl(peer2));
		peer3 = new ClientImpl("127.0.0.1", 2, new MessageListenerImpl(peer3));
		peer4 = new ClientImpl("127.0.0.1", 3, new MessageListenerImpl(peer4));
		
	}

	@AfterEach
	public void terminate() {

		peer1.shutdown();
		peer2.shutdown();
		peer3.shutdown();
		peer4.shutdown();
		peer1 = null;
		peer2 = null;
		peer3 = null;
		peer4 = null;

	}

	// Login tests
	
	@Test
	void testCase_Peer1LoginWithoutMasterLoggedIn() throws Exception {

		peer1.shutdown();
		peer2.shutdown();
		peer3.shutdown();
		peer4.shutdown();
		peer1 = null;
		peer2 = null;
		peer3 = null;
		peer4 = null;
		assertThrows(MasterPeerNotFoundException.class, () -> new ClientImpl("127.0.0.1", 1, new MessageListenerImpl(peer2)));
		
		peer1 = new ClientImpl("127.0.0.1", 0, new MessageListenerImpl(peer1));
		peer2 = new ClientImpl("127.0.0.1", 1, new MessageListenerImpl(peer2));
		peer3 = new ClientImpl("127.0.0.1", 2, new MessageListenerImpl(peer3));
		peer4 = new ClientImpl("127.0.0.1", 3, new MessageListenerImpl(peer4));

	}

	@Test
	void testCase_Checklogin() throws Exception {

		assertTrue(peer1.checkPlayer("lorenzo"));
		assertTrue(peer2.checkPlayer("mario"));
		assertTrue(peer3.checkPlayer("dario"));
		assertTrue(peer4.checkPlayer("marcello"));

		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());
		assertTrue(peer3.leaveNetwork());
		assertTrue(peer4.leaveNetwork());

	}

	@Test
	void testCase_NicknameAlreadyExists() throws Exception{

		peer1.checkPlayer("lorenzo");

		assertFalse(peer2.checkPlayer("lorenzo"));
		assertTrue(peer1.leaveNetwork());

	}

	@Test
	void testCase_PeerAlreadyLoggedIn() throws Exception{

		peer1.checkPlayer("lorenzo");

		Exception exception = assertThrows(RuntimeException.class, () -> {
			peer1.checkPlayer("mario");
		});

		assertEquals(exception.getMessage(), "Player già presente.");
		assertTrue(peer1.leaveNetwork());
	}


	@Test
	void testCase_NicknameUnavailable() throws Exception{

		assertFalse(peer1.checkPlayer("lor enzo"));

	}


	// Challenges tests

	@Test
	void testCase_GenerateNewSudokuChallenge() throws Exception{
		peer1.checkPlayer("lorenzo");
		assertTrue(peer1.generateNewSudoku("challenge1", 0));
		assertNotNull(peer1.getChallenge());
		assertTrue(peer1.leaveNetwork());

	}

	@Test
	void testCase_FindChallenge() throws Exception{

		peer1.checkPlayer("lorenzo");
		assertTrue(peer1.generateNewSudoku("challenge1", 0));
		assertEquals(peer1.findChallenge(), 0);
		assertTrue(peer1.leaveNetwork());
	}

	@Test
	void testCase_CodicePartitaAlreadyExists() throws Exception{

		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");

		peer1.generateNewSudoku("challenge1", 0);
		peer2.reloadChallengeList();


		assertFalse(peer2.generateNewSudoku("challenge1", 0));
		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());

	}

	@Test
	void testCase_AddChallengeToChallengesList() throws Exception{
		peer1.checkPlayer("lorenzo");

		peer1.generateNewSudoku("challenge1", 0);

		assertTrue(peer1.getChallenges().size()!=0);
		assertTrue(peer1.leaveNetwork());
	}

	@Test
	void testCase_RemoveChallengeFromChallengesList() throws Exception{
		peer1.checkPlayer("lorenzo");

		peer1.generateNewSudoku("challenge1", 0);

		assertTrue(peer1.getChallenges().size()!=0);

		peer1.removeFromChallengeList();

		assertTrue(peer1.getChallenges().size()==0);

		assertTrue(peer1.leaveNetwork());
	}

	@Test
	void testCase_JoinToChallenge() throws Exception{

		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");

		peer1.generateNewSudoku("challenge1", 0);
		peer2.reloadChallengeList();
		peer2.joinChallenge("challenge1");

		assertEquals(peer1.getChallenge().getCodice_partita(), peer2.getChallenge().getCodice_partita());
		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());

	}
	@Test
	void testCase_JoinToChallengeNotYetPresent() throws Exception{

		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");

		peer1.generateNewSudoku("challenge1", 0);
		assertFalse(peer2.joinChallenge("challenge1"));

		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());

	}

	@Test
	void testCase_StartChallenge() throws Exception{

		peer1.checkPlayer("lorenzo");

		peer1.generateNewSudoku("challenge1", 0);

		assertFalse(peer1.getChallenge().isStarted());

		peer1.startChallenge("challenge1");

		assertTrue(peer1.getChallenge().isStarted());

		assertTrue(peer1.leaveNetwork());

	}

	@Test
	void testCase_LeaveNetwork() throws Exception{

		peer1.checkPlayer("lorenzo");
		peer1.leaveNetwork();
		assertTrue(peer2.checkPlayer("lorenzo"));

		assertTrue(peer2.leaveNetwork());

	}

	// Game tests

	@Test 
	void testCase_QuitChallenge() throws Exception{

		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");

		peer1.generateNewSudoku("challenge1", 0);
		peer2.reloadChallengeList();
		peer2.joinChallenge("challenge1");

		assertTrue(peer2.quitChallenge("challenge1"));
		assertTrue(peer1.quitChallenge("challenge1"));
		
		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());


	}


	@Test
	void testCase_PlaceCorrectNumber() throws Exception{
		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");

		peer1.generateNewSudoku("challenge1", -1);
		peer2.reloadChallengeList();
		peer2.joinChallenge("challenge1");
		peer1.reloadChallenge("challenge1");

		assertEquals(peer1.placeNumber("challenge1", 0, 0, 8), 1);
		
		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());


	}

	@Test
	void testCase_PlaceWrongNumber() throws Exception{
		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");

		peer1.generateNewSudoku("challenge1", -1);
		peer2.reloadChallengeList();
		peer2.joinChallenge("challenge1");
		peer1.reloadChallenge("challenge1");

		assertEquals(peer1.placeNumber("challenge1", 0, 0, 1), -1);
		
		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());
		

	}

	@Test
	void testCase_PlaceAlreadyPlacedNumber() throws Exception{
		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");

		peer1.generateNewSudoku("challenge1", -1);
		peer2.reloadChallengeList();
		peer2.joinChallenge("challenge1");
		peer1.reloadChallenge("challenge1");

		assertEquals(peer1.placeNumber("challenge1", 0, 1, 4), 0);
		
		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());

		

	}

	@Test
	void testCase_SudokuTerminated() throws Exception{

		peer1.checkPlayer("lorenzo");
		peer2.checkPlayer("mario");
		peer3.checkPlayer("dario");
		peer4.checkPlayer("marcello");

		peer1.generateNewSudoku("challenge1", -1);
		peer2.reloadChallengeList();
		peer3.reloadChallengeList();
		peer4.reloadChallengeList();
		
		peer2.joinChallenge("challenge1");
		peer3.joinChallenge("challenge1");
		peer4.joinChallenge("challenge1");
		
		peer1.placeNumber("challenge1", 0, 0, 8);
		peer2.placeNumber("challenge1", 0, 2, 3);
		
		peer3.placeNumber("challenge1", 0, 4, 7);
		
		peer3.placeNumber("challenge1", 0, 6, 6);
		
		peer4.placeNumber("challenge1", 0, 8, 2);

		peer2.placeNumber("challenge1", 1, 1, 1);
		peer1.placeNumber("challenge1", 1, 3, 8);
		peer1.placeNumber("challenge1", 1, 5, 2);
		peer3.placeNumber("challenge1", 1, 7, 4);

		peer4.placeNumber("challenge1", 2, 0, 6);
		peer2.placeNumber("challenge1", 2, 2, 2);
		peer1.placeNumber("challenge1", 2, 4, 4);
		peer3.placeNumber("challenge1", 2, 6, 1);
		peer3.placeNumber("challenge1", 2, 8, 7);

		peer1.placeNumber("challenge1", 3, 1, 3);
		peer1.placeNumber("challenge1", 3, 3, 1);
		peer2.placeNumber("challenge1", 3, 5, 5);
		peer2.placeNumber("challenge1", 3, 7, 7);

		peer4.placeNumber("challenge1", 4, 0, 2);
		peer3.placeNumber("challenge1", 4, 2, 5);
		peer2.placeNumber("challenge1", 4, 4, 3);
		peer1.placeNumber("challenge1", 4, 6, 4);
		peer1.placeNumber("challenge1", 4, 8, 6);

		peer4.placeNumber("challenge1", 5, 1, 9);
		peer4.placeNumber("challenge1", 5, 3, 4);
		peer1.placeNumber("challenge1", 5, 5, 6);
		peer1.placeNumber("challenge1", 5, 7, 2);

		peer2.placeNumber("challenge1", 6, 0, 5);
		peer1.placeNumber("challenge1", 6, 2, 9);
		peer2.placeNumber("challenge1", 6, 4, 1);
		peer3.placeNumber("challenge1", 6, 6, 2);
		peer1.placeNumber("challenge1", 6, 8, 4);

		peer4.placeNumber("challenge1", 7, 1, 6);
		peer1.placeNumber("challenge1", 7, 3, 2);
		peer2.placeNumber("challenge1", 7, 5, 7);
		peer1.placeNumber("challenge1", 7, 7, 3);

		peer2.placeNumber("challenge1", 8, 0, 3);
		peer1.placeNumber("challenge1", 8, 2, 8);
		peer1.placeNumber("challenge1", 8, 4, 9);
		peer2.placeNumber("challenge1", 8, 6, 7);

		assertFalse(peer1.getChallenge().isFull());
		assertFalse(peer1.getChallenge().isTerminated());

		assertEquals(peer1.placeNumber("challenge1", 8, 8, 1), 1);

		assertTrue(peer1.getChallenge().isFull());
		assertTrue(peer1.getChallenge().isTerminated());
		
		peer2.reloadChallenge("challenge1");
		peer3.reloadChallenge("challenge1");
		peer4.reloadChallenge("challenge1");
		
		assertTrue(peer2.getChallenge().isFull());
		assertTrue(peer2.getChallenge().isTerminated());
		
		assertTrue(peer3.getChallenge().isFull());
		assertTrue(peer3.getChallenge().isTerminated());
		
		assertTrue(peer4.getChallenge().isFull());
		assertTrue(peer4.getChallenge().isTerminated());
		
		assertTrue(peer1.leaveNetwork());
		assertTrue(peer2.leaveNetwork());
		assertTrue(peer3.leaveNetwork());
		assertTrue(peer4.leaveNetwork());


	}
	
	
	
	

}
