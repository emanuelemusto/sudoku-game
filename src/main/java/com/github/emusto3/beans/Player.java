package com.github.emusto3.beans;

import java.io.Serializable;

import net.tomp2p.peers.PeerAddress;

public class Player implements Serializable{
	
	private String nickname;
	private PeerAddress peerAdd;
	
	
	public Player(String nickname, PeerAddress peerAdd) {
		this.nickname = nickname;
		this.peerAdd = peerAdd;
	}
	
	public Player(String nickname) {
		this.nickname = nickname;
	}
	
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public PeerAddress getPeerAdd() {
		return peerAdd;
	}
	public void setPeerAdd(PeerAddress peerAdd) {
		this.peerAdd = peerAdd;
	}
	
	
	

}
