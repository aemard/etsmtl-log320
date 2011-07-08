package ca.etsmtl.breaktrough;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ca.etsmtl.breaktrough.NinjaClient.Player;

public class TheBrain extends Thread {

	private GameTable gameTable;
	private int deepnessTree;
	private Move myBestMove;
	private Move mySuggestedMove;
	
	private Move tempChoosedMove;
	private int tempChoosedMovePoint;
	
	private static Hashtable<CompositeKey, Move> whatIShouldPlay = new Hashtable<CompositeKey, Move>();
	private static Hashtable<CompositeKey, Integer> whatIShouldPlayPoint = new Hashtable<CompositeKey, Integer>();
	private static Hashtable<CompositeKey, Integer> whatIShouldPlayDeepness = new Hashtable<CompositeKey, Integer>();
	
	private Player maxPlayer;
	private Player minPlayer;
	
	private boolean running = false;
	
	private int brainId;
	
	public TheBrain(Player maxPlayer, Player minPlayer, int brainId) {
		gameTable = null;
		deepnessTree = 0;
		myBestMove = null;
		tempChoosedMove = null;
		tempChoosedMovePoint = 0;
		
		this.maxPlayer = maxPlayer;
		this.minPlayer = minPlayer;
		this.brainId = brainId;
	}
	
	public Move getBestMove() {
		return myBestMove;
	}
	
	public Move mySuggestedMove() {
		return mySuggestedMove;
	}
	
	public int howSmartAmI() {
		return deepnessTree -1;
	}
	
	public void run() {
		//System.out.println("Brain " + brainId + " is running");
		while (running) {
			NinjaMax(gameTable, deepnessTree,Integer.MIN_VALUE,Integer.MAX_VALUE);
			deepnessTree = deepnessTree + 1;
		}
		//System.out.println("Brain " + brainId + " is stopped");
	}
	
	public void stopBrain() {
		running = false;
	}
	
	public void prepareTheBrain(GameTable gameTable, int deepnessTree) {
		running = true;
		this.gameTable = new GameTable(gameTable);
		this.deepnessTree = deepnessTree;
		myBestMove = null;
	}
	
	private int NinjaMax(GameTable table, int howManyMovesLeft, int alpha, int beta) {
		Move tempBestMove = null;
		
		if (isLastMove(howManyMovesLeft) || !running) {
			return evalTable(table, maxPlayer);
		}
		
		List<Move> listMove = new ArrayList<Move>();

		int deepnessSuggested = 0; 
		// Try the move we suggested for this attack
		if (isMyFirstMove(howManyMovesLeft)) {
			// We are ready man!
			Move move = null;
			int pointSuggested = 0;
			CompositeKey key = new CompositeKey(table.getTable(maxPlayer),table.getTable(minPlayer));
			if (whatIShouldPlay.containsKey(key)) {
				move = whatIShouldPlay.get(key);
				pointSuggested = whatIShouldPlayPoint.get(key);
				deepnessSuggested = whatIShouldPlayDeepness.get(key);
				
				// TODO: To be removed
				if (!table.isValidMove(move)) {
					System.out.println("Damn you. I can't play that.");
					GameTable.printTable(table.getTable());
				}
				/*if (isSuicidal(table, move)) {
					System.out.println("Damn you. You suggest me to suicide.");
					GameTable.printTable(table.getTable());
				}*/
				mySuggestedMove = move;
				listMove.add(move);	
			
				if (deepnessSuggested < deepnessTree) {
					System.out.println("I have been suggest to do this move: " + move.toString());
				}
			}

			// Let's clear the table. But we want to keep our best move in case
			// we have enough time to go one level deeper.
			whatIShouldPlay.clear();
			whatIShouldPlayPoint.clear();
			whatIShouldPlayDeepness.clear();
			if (move !=  null) {
				whatIShouldPlay.put(key, move);
				whatIShouldPlayPoint.put(key, pointSuggested);
				whatIShouldPlayDeepness.put(key, deepnessSuggested);
			}
		}
		
		int currentAlpha = Integer.MIN_VALUE;
		
		// If the answer is more precise than our research
		if (deepnessSuggested >= deepnessTree) {
			System.out.println("The given problem is level " + deepnessTree + ". TheBrain already had a solution for level " + deepnessSuggested + ".");
			tempBestMove = listMove.get(0);
		// We have to check our answer
		} else {
			listMove.addAll(GameTable.getAllMove(table, maxPlayer));
			
			for (Move move : listMove) {
				GameTable newGameTable = new GameTable(table);	
				newGameTable.move(move);
				
				// If this move is suicidal let's not do it.
				/*if (isSuicidal(table, move)) {
					continue;
				}*/
				
				int score = NinjaMin(newGameTable, howManyMovesLeft-1,Math.max(alpha,currentAlpha),beta);
				
				if (table.getTableCount(minPlayer) > newGameTable.getTableCount(minPlayer)) {
					score += 50;
				}
				
				// This is the best score so far						
				if (score > currentAlpha) {
					currentAlpha = score;
					tempBestMove = move;
					
					// This is our best move so far for this table
					if (isMySecondMove(howManyMovesLeft)) {
						tempChoosedMove = tempBestMove;
						tempChoosedMovePoint = currentAlpha;
					}
					
					// In fact, I can play so good that I don't expect the opponent to 
					// be enough stupid do the last move, let just leave.
					if (currentAlpha >= beta) {					
						return currentAlpha;
					}
				}
			}
		}
		
		// Let save the move we should play
		if (isMyFirstMove(howManyMovesLeft)) {
			myBestMove = tempBestMove;
			
			if (deepnessSuggested < deepnessTree) {
				System.out.println("Hey. TheBrain " + brainId + " solved the problem for level " + deepnessTree);
			}
		}
		
		return currentAlpha;
	}
	
	private int NinjaMin(GameTable table, int howManyMovesLeft, int alpha, int beta) {	
		if (isLastMove(howManyMovesLeft) || !running) {
			return evalTable(table, maxPlayer);
		}
			
		List<Move> listMove = GameTable.getAllMove(table, minPlayer);
		
		int currentBeta = Integer.MAX_VALUE;
		for (Move move : listMove) {
			GameTable newGameTable = new GameTable(table);
	
			newGameTable.move(move);
			int score = NinjaMax(newGameTable, howManyMovesLeft-1,alpha,Math.min(beta,currentBeta));
			
			if (table.getTableCount(maxPlayer) > newGameTable.getTableCount(maxPlayer)) {
				score -= 50;
			}
			
			// Let save the best move if we face that table
			if (isOppFirstMove(howManyMovesLeft)){
				CompositeKey key = new CompositeKey(newGameTable.getTable(maxPlayer),newGameTable.getTable(minPlayer));
				whatIShouldPlay.put(key, new Move(tempChoosedMove));
				whatIShouldPlayPoint.put(key, tempChoosedMovePoint);
				whatIShouldPlayDeepness.put(key, deepnessTree-2);
			}
			
			// This is the best score so far
			if (score < currentBeta) {
				currentBeta = score;
				
				// In fact, he can play so good that I will never do that
				// last move, let just leave.
				if (currentBeta <= alpha) {
					return currentBeta;
				}
			}
		}
		
		return currentBeta;
	}

	// D�sol� Simon, mais � partir d'aujourd'hui il est interdit � notre robot
	// de "cr�er des ouvertures" :p Il est un peu trop con pour ce genre d'initiative
	private boolean isSuicidal(GameTable table, Move move) {
		if (table.isInDanger(move.finalPos, maxPlayer)) {
			if ((table.getTable(minPlayer) & move.finalPos) == 0) {
				return true;
			}	
		}
		return false;
	}
	
	// TODO: To be removed
	// We should never get stuck in this function.
	private void isValid(GameTable newGameTable, Move move) {
		if (!newGameTable.isValidMove(move)) {
			while (true) {
				System.out.println("INVALID MOVE");
				GameTable.printGameTable(newGameTable);
				System.out.println(move.toString());
			}
		}
	}
	
	private boolean isMyFirstMove(int howManyMovesLeft) {
		return howManyMovesLeft == deepnessTree;
	}

	private boolean isMySecondMove(int howManyMovesLeft) {
		return howManyMovesLeft == deepnessTree-2;
	}
	
	private boolean isOppFirstMove(int howManyMovesLeft) {
		return howManyMovesLeft == deepnessTree-1;
	}
	
	private boolean isLastMove(int howManyMovesLeft) {
		return howManyMovesLeft == 0;
	}
	
	private int evalTable(GameTable table, Player player) {
		return table.getTableScore(player);
	}

}