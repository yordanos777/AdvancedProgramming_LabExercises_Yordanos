import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;


class Card {
    public enum Suit {
        HEARTS, DIAMONDS, CLUBS, SPADES
    }

    public enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), 
        NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14);
        private final int value;
        Rank(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    private final Suit suit;
    private final Rank rank;

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public Suit getSuit() { return suit; }
    public Rank getRank() { return rank; }

    @Override
    public String toString() {
        return rank.name() + " of " + suit.name();
    }
}


class Deck {
    private final List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        reset();
    }

    public void reset() {
        cards.clear();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(cards);
    }

    public Card dealCard() {
        if (cards.isEmpty()) throw new IllegalStateException("Deck is empty!");
        return cards.remove(0);
    }
}


class PokerHandEvaluator {
    public static int evaluateHand(List<Card> holeCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>(holeCards);
        allCards.addAll(communityCards);
        
        Map<Card.Rank, Long> rankCounts = allCards.stream()
                .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));

        if (rankCounts.containsValue(4L)) return 800; 
        if (rankCounts.containsValue(3L) && rankCounts.containsValue(2L)) return 700; 
        if (rankCounts.containsValue(3L)) return 400; 
        
        long pairCount = rankCounts.values().stream().filter(count -> count == 2).count();
        if (pairCount >= 2) return 300; 
        if (pairCount == 1) return 200; 

        return allCards.stream().mapToInt(c -> c.getRank().getValue()).max().orElse(0); 
    }
}


public class PokerApp extends Application {

    private Deck deck;
    private List<Card> playerHand;
    private List<Card> aiHand;
    private List<Card> communityCards;

    private int pot = 0;
    private int playerChips = 1000;
    private int aiChips = 1000;
    private int currentBet = 0;
    private int gamePhase = 0; 

    private HBox playerCardBox;
    private HBox aiCardBox;
    private HBox communityCardBox;
    private Label statusLabel;
    private Label potLabel;
    private Label playerStatsLabel;
    private Label aiStatsLabel;

    private Button btnCall;
    private Button btnRaise;
    private Button btnFold;
    private Button btnDeal;

    @Override
    public void start(Stage primaryStage) {
        deck = new Deck();
        playerHand = new ArrayList<>();
        aiHand = new ArrayList<>();
        communityCards = new ArrayList<>();

        BorderPane root = new BorderPane();

        
        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10));
        aiStatsLabel = new Label("AI Chips: $1000");
        aiCardBox = new HBox(10);
        aiCardBox.setAlignment(Pos.CENTER);
        topBox.getChildren().addAll(aiStatsLabel, aiCardBox);
        root.setTop(topBox);

        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        potLabel = new Label("POT: $0");
        statusLabel = new Label("Press Deal to start a new hand.");
        communityCardBox = new HBox(10);
        communityCardBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(potLabel, communityCardBox, statusLabel);
        root.setCenter(centerBox);

        VBox bottomBox = new VBox(10);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(10));

        playerCardBox = new HBox(10);
        playerCardBox.setAlignment(Pos.CENTER);
        playerStatsLabel = new Label("Your Chips: $1000");

        HBox actionControls = new HBox(10);
        actionControls.setAlignment(Pos.CENTER);

        btnCall = new Button("Check / Call");
        btnRaise = new Button("Raise $50");
        btnFold = new Button("Fold");
        btnDeal = new Button("Deal Hand");

        setGameplayButtonsDisable(true);

        actionControls.getChildren().addAll(btnCall, btnRaise, btnFold, btnDeal);
        bottomBox.getChildren().addAll(playerCardBox, playerStatsLabel, actionControls);
        root.setBottom(bottomBox);

        
        btnDeal.setOnAction(e -> startNewHand());
        btnCall.setOnAction(e -> processTurn(Action.CALL));
        btnRaise.setOnAction(e -> processTurn(Action.RAISE));
        btnFold.setOnAction(e -> processTurn(Action.FOLD));

        Scene scene = new Scene(root, 700, 500);
        primaryStage.setTitle("Poker Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private enum Action { CALL, RAISE, FOLD }

    private void startNewHand() {
        deck.reset();
        playerHand.clear();
        aiHand.clear();
        communityCards.clear();
        pot = 0;
        gamePhase = 0;
        currentBet = 0;

        playerChips -= 10;
        aiChips -= 10;
        pot += 20;

        playerHand.add(deck.dealCard());
        playerHand.add(deck.dealCard());
        aiHand.add(deck.dealCard());
        aiHand.add(deck.dealCard());

        updateUI();
        statusLabel.setText("Pre-flop: Choose your move.");
        setGameplayButtonsDisable(false);
        btnDeal.setDisable(true);
    }

    private void processTurn(Action playerAction) {
        if (playerAction == Action.FOLD) {
            aiChips += pot;
            statusLabel.setText("You folded. AI wins.");
            endHand();
            return;
        }

        if (playerAction == Action.RAISE) {
            playerChips -= 50;
            pot += 50;
            currentBet += 50;
        }

        if (currentBet > 0) {
            aiChips -= currentBet;
            pot += currentBet;
            currentBet = 0;
        }

        gamePhase++;
        advanceGamePhase();
    }

    private void advanceGamePhase() {
        switch (gamePhase) {
            case 1 -> {
                communityCards.add(deck.dealCard());
                communityCards.add(deck.dealCard());
                communityCards.add(deck.dealCard());
                statusLabel.setText("The Flop.");
            }
            case 2 -> {
                communityCards.add(deck.dealCard());
                statusLabel.setText("The Turn.");
            }
            case 3 -> {
                communityCards.add(deck.dealCard());
                statusLabel.setText("The River.");
            }
            case 4 -> {
                evaluateShowdown();
                return;
            }
        }
        updateUI();
    }

    private void evaluateShowdown() {
        updateUI(); 
        int playerScore = PokerHandEvaluator.evaluateHand(playerHand, communityCards);
        int aiScore = PokerHandEvaluator.evaluateHand(aiHand, communityCards);

        aiCardBox.getChildren().clear();
        for (Card card : aiHand) {
            aiCardBox.getChildren().add(new Label("[" + card.toString() + "]"));
        }

        if (playerScore > aiScore) {
            playerChips += pot;
            statusLabel.setText("You win!");
        } else if (aiScore > playerScore) {
            aiChips += pot;
            statusLabel.setText("AI wins.");
        } else {
            playerChips += pot / 2;
            aiChips += pot / 2;
            statusLabel.setText("Tie game.");
        }
        endHand();
    }

    private void endHand() {
        pot = 0;
        setGameplayButtonsDisable(true);
        btnDeal.setDisable(false);
    }

    private void updateUI() {
        potLabel.setText("POT: $" + pot);
        playerStatsLabel.setText("Your Chips: $" + playerChips);
        aiStatsLabel.setText("AI Chips: $" + aiChips);

        playerCardBox.getChildren().clear();
        for (Card card : playerHand) {
            playerCardBox.getChildren().add(new Label("[" + card.toString() + "]"));
        }

        aiCardBox.getChildren().clear();
        if (gamePhase < 4) {
            aiCardBox.getChildren().add(new Label("[Hidden Card]"));
            aiCardBox.getChildren().add(new Label("[Hidden Card]"));
        }

        communityCardBox.getChildren().clear();
        for (Card card : communityCards) {
            communityCardBox.getChildren().add(new Label("[" + card.toString() + "]"));
        }
    }

    private void setGameplayButtonsDisable(boolean disable) {
        btnCall.setDisable(disable);
        btnRaise.setDisable(disable);
        btnFold.setDisable(disable);
    }

    public static void main(String[] args) {
        launch(args);
    }
}