package com.example.rdhol.mineseeker;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.security.InvalidParameterException;
import java.util.Random;
import static android.R.id.edit;
import static com.example.rdhol.mineseeker.Options.BOARD_SIZE_OPTION_KEY;
import static com.example.rdhol.mineseeker.Options.MINE_NUM_KEY;
import static com.example.rdhol.mineseeker.Options.OPTIONS_PREFS_KEY;
import static com.example.rdhol.mineseeker.R.id.textView;
import static java.lang.reflect.Array.getInt;
public class PlayGameActivity extends AppCompatActivity {
    public static final String BEST_SCORE = "BEST_SCORE";
    public static final String REPLACE_BEST_SCORE = "REPLACE_BEST_SCORE";
    public static final String GAMES_PLAYED = "GAMES_PLAYED";
    public static final String GET_GAMES_PLAYED = "GET_GAMES_PLAYED";
    int numOfCols;
    int numOfRows;
    private GameCell[][] gameCells;
    private int numOfTreasures;
    private int numOfScansUsed;
    private int numOfTreasuresFound;
    private Vibrator vibrator;
    public static Intent makeIntent(Context context) {
        return new Intent(context, PlayGameActivity.class);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_game);
        loadBestScore();
        loadNumberOfGames();
        loadNumOfMines();
        loadBoardRow();
        loadBoardCol();
        setupGameCells();
        vibrator = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }
    private void setupGameCells() {
        //TODO: Add error handling for loading/saving
        //TODO: MOVE code into a separate class, this method is way to large
        //TODO: HANDLE saving  on exit
        numOfTreasuresFound = 0;
        numOfScansUsed = 0;
        numOfRows = loadBoardRow();
        numOfCols = loadBoardCol();
        gameCells = new GameCell[numOfRows][numOfCols];
        TableLayout cells = (TableLayout) findViewById(R.id.tableForGameCells);
        for (int row = 0; row < numOfRows; row++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1.0f));
            cells.addView(tableRow);
            for (int col = 0; col < numOfCols; col++) {
                Button button = new Button(this);
                button.setLayoutParams(new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.MATCH_PARENT,
                        1.0f));
                //no padding to reduce text clipping
                button.setPadding(0, 0, 0, 0);
                final int finalRow = row;
                final int finalCol = col;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gameCellClicked(finalCol, finalRow);
                    }
                });
                GameCell gameCell = new GameCell(button);
                gameCells[row][col] = gameCell;
                tableRow.addView(button);
            }
        }
        //randomly place treasure
        numOfTreasures = loadNumOfMines();
        if (numOfTreasures > (numOfRows * numOfCols)) {
            throw new InvalidParameterException();
        }
        Random rand = new Random();
        for (int i = 0; i < numOfTreasures; i++) {
            int rand1 = rand.nextInt(numOfRows);
            int rand2 = rand.nextInt(numOfCols);
            //if we try to add a treasure to a cell that is full, decrement to ensure correctness
            if (!gameCells[rand1][rand2].tryGiveTreasure()) {
                i--;
            }
        }
        updateUI();
    }
    private void gameCellClicked(int col, int row) {
        lockButtonSizes();
        final GameCell gameCellClicked = gameCells[row][col];
        //scanForTreasure has a side effect of turning the gamecell
        // into a scanpoint if there is no treasure
        boolean isScanPoint = gameCellClicked.isScanPoint();
        boolean treasureFound = gameCellClicked.scanForTreasure(this);
        if (treasureFound) {
            numOfTreasuresFound++;
            playTreasureFoundSound();
            vibrate(300);
        } else if (!isScanPoint) {
            numOfScansUsed++;
            playScanAnimation(col, row);
            playScanSound();
            vibrate(50);
        }
        updateUI();
        if (numOfTreasuresFound >= numOfTreasures) {
            saveBestScore();
            saveNumberOfGames();
            displayWinDialog();
        }
    }
    private void vibrate(int durationInMilliseconds) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(durationInMilliseconds);
        }
    }
    private void playTreasureFoundSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.treasure_found);
        mediaPlayer.start();
    }
    private void playScanSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.scan);
        mediaPlayer.start();
    }
    private void playScanAnimation(int colOfStartingCell, int rowOfStartingCell) {
        GameCell cellToAnimate = gameCells[rowOfStartingCell][colOfStartingCell];
        cellToAnimate.playScanAnimation(this.getApplicationContext());
        for (int col = 0; col < colOfStartingCell; col++) {
            cellToAnimate = gameCells[rowOfStartingCell][col];
            cellToAnimate.playScanAnimation(this.getApplicationContext());
        }
        for (int col = colOfStartingCell; col < gameCells[0].length; col++) {
            cellToAnimate = gameCells[rowOfStartingCell][col];
            cellToAnimate.playScanAnimation(this.getApplicationContext());
        }
        for (int row = 0; row < rowOfStartingCell; row++) {
            cellToAnimate = gameCells[row][colOfStartingCell];
            cellToAnimate.playScanAnimation(this.getApplicationContext());
        }
        for (int row = rowOfStartingCell; row < gameCells.length; row++) {
            cellToAnimate = gameCells[row][colOfStartingCell];
            cellToAnimate.playScanAnimation(this.getApplicationContext());
        }
    }
    private void updateUI() {
        TextView txtNumOfTreasuresFound = (TextView) findViewById(R.id.txtNumOfTreasuresFound);
        txtNumOfTreasuresFound.setText(numOfTreasuresFound + " of " +
                numOfTreasures + " treasures found");
        TextView txtNumOfScansUsed = (TextView) findViewById(R.id.txtNumOfScansUsed);
        txtNumOfScansUsed.setText(numOfScansUsed + " Scans used");
        //TODO: move gameCell array to a gameBoard class
        //go through the gameCells and update UI of scan points
        for (int row = 0; row < gameCells.length; row++) {
            for (int col = 0; col < gameCells[row].length; col++) {
                if (gameCells[row][col].isScanPoint()) {
                    updateScanPoints(col, row);
                }
            }
        }
    }
    private void updateScanPoints(int col, int row) {
        int numOfTreasureFoundInScan = 0;
        for (int i = 0; i < row; i++) {
            if (gameCells[i][col].hasHiddenTreasure()) {
                numOfTreasureFoundInScan++;
            }
        }
        for (int i = row; i < numOfRows; i++) {
            if (gameCells[i][col].hasHiddenTreasure()) {
                numOfTreasureFoundInScan++;
            }
        }
        for (int i = 0; i < col; i++) {
            if (gameCells[row][i].hasHiddenTreasure()) {
                numOfTreasureFoundInScan++;
            }
        }
        for (int i = row; i < numOfCols; i++) {
            if (gameCells[row][i].hasHiddenTreasure()) {
                numOfTreasureFoundInScan++;
            }
        }
        gameCells[row][col].displayText("" + numOfTreasureFoundInScan);
    }
    private void lockButtonSizes() {
        //lock button sizes
        for (int row = 0; row < numOfRows; row++) {
            for (int col = 0; col < numOfCols; col++) {
                gameCells[row][col].lockButtonSize();
            }
        }
    }
    private int loadNumOfMines() {
        int numOfTreasure;
        SharedPreferences sharedPref;
        sharedPref = getSharedPreferences(OPTIONS_PREFS_KEY, Context.MODE_PRIVATE);
        int mineSpinVal = sharedPref.getInt(MINE_NUM_KEY, Options.SIX_MINES);
        switch (mineSpinVal) {
            case Options.SIX_MINES:
                numOfTreasure = 6;
                break;
            case Options.TEN_MINES:
                numOfTreasure = 10;
                break;
            case Options.FIFTEEN_MINES:
                numOfTreasure = 15;
                break;
            case Options.TWENTY_MINES:
                numOfTreasure = 20;
                break;
            default:
                numOfTreasure = 6;
                break;
        }
        return numOfTreasure;
    }
    private int loadBoardRow() {
        int numOfRows;
        SharedPreferences sharedPref = getSharedPreferences(OPTIONS_PREFS_KEY, Context.MODE_PRIVATE);
        int boardSpinVal = sharedPref.getInt(BOARD_SIZE_OPTION_KEY, Options.SIX_BY_FIFTEEN);
        switch (boardSpinVal) {
            case Options.FOUR_BY_SIX:
                numOfRows = 4;
                break;
            case Options.FIVE_BY_TEN:
                numOfRows = 5;
                break;
            case Options.SIX_BY_FIFTEEN:
                numOfRows = 6;
                break;
            default:
                throw new InvalidParameterException();
        }
        return numOfRows;
    }
    private int loadBoardCol() {
        int numOfCols;
        SharedPreferences sharedPref = getSharedPreferences(OPTIONS_PREFS_KEY, Context.MODE_PRIVATE);
        int boardSpinVal = sharedPref.getInt(BOARD_SIZE_OPTION_KEY, Options.SIX_BY_FIFTEEN);
        switch (boardSpinVal) {
            case Options.FOUR_BY_SIX:
                numOfCols = 6;
                break;
            case Options.FIVE_BY_TEN:
                numOfCols = 10;
                break;
            case Options.SIX_BY_FIFTEEN:
                numOfCols = 15;
                break;
            default:
                throw new InvalidParameterException();
        }
        return numOfCols;
    }
    private void displayWinDialog() {
        FragmentManager manager = getSupportFragmentManager();
        WinDialog dialog = new WinDialog();
        dialog.show(manager, "MessageDialog");
        Log.i("TAG", "jUST SHOWD DIALOG");
        // finish();
    }
    private void saveBestScore() {
        SharedPreferences sharedPref = getSharedPreferences(BEST_SCORE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String bestScoreString = sharedPref.getString(REPLACE_BEST_SCORE, "0");
        int bestScore = Integer.parseInt(bestScoreString);
        if (bestScore <= 0) {
            String newBestScore = Integer.toString(numOfScansUsed);
            editor.putString(REPLACE_BEST_SCORE, newBestScore);
            editor.apply();
        }
        if (numOfScansUsed < bestScore) {
            String newBestScore = Integer.toString(numOfScansUsed);
            editor.putString(REPLACE_BEST_SCORE, newBestScore);
            editor.apply();
        }
    }
    private void loadBestScore() {
        SharedPreferences loadBestScore = getSharedPreferences(BEST_SCORE, Context.MODE_PRIVATE);
        String bestScore = loadBestScore.getString(REPLACE_BEST_SCORE, "0");
        TextView textView = (TextView) findViewById(R.id.txt_BestScore);
        textView.setText(bestScore);
    }
    private void saveNumberOfGames() {
        SharedPreferences sharedPref = getSharedPreferences(GAMES_PLAYED, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        int gamesPlayed = sharedPref.getInt(GET_GAMES_PLAYED, 0);
        gamesPlayed += 1;
        editor.putInt(GET_GAMES_PLAYED, gamesPlayed);
        editor.apply();
    }
    private void loadNumberOfGames() {
        SharedPreferences loadGamesPlayed = getSharedPreferences(GAMES_PLAYED, Context.MODE_PRIVATE);
        int numGamesPlayed = loadGamesPlayed.getInt(GET_GAMES_PLAYED, 0);
        TextView textView = (TextView) findViewById(R.id.txt_GamesPlayed);
        String numGamesPlayedString = String.valueOf(numGamesPlayed);
        textView.setText(numGamesPlayedString);
    }
}