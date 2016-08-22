package com.antbrains.sc.tools;

import com.antbrains.sc.data.Block;

public class BlockInfo {
	public String getParentUrl() {
		return parentUrl;
	}

	public Block getBlock() {
		return block;
	}

	private String parentUrl;
	private Block block;
	private int pos;

	public int getPos() {
		return pos;
	}

	public BlockInfo(String pUrl, Block b, int pos) {
		parentUrl = pUrl;
		block = b;
		this.pos = pos;
	}
}
