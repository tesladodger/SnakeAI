package com.tesladodger.snake;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.tesladodger.snake.objects.Snake;
import com.tesladodger.snake.objects.Food;
//import com.tesladodger.snake.ai.AStar;
import com.tesladodger.snake.ai.SnakeAI;

public class SnakeGame extends ApplicationAdapter {

    private Random ran = new Random();

    private Snake snake;
    private Food food;

    private SnakeAI snakeAI;
    private List<Integer> moveArray;
    private int move;

    private ShapeRenderer shapeRenderer;

    private boolean buttonPressed;
    private boolean ateTail;

    private boolean aiMode;

    // From the config file
    private int hs;
    private int delay;
    private int fontSize;
    private boolean showFPS;

    private Stage stage;
    private FreeTypeFontGenerator ftfg;
    private Label scoreLabel;
    private StringBuilder strB;

    private long startTime;

    @Override
    public void create() {

        shapeRenderer = new ShapeRenderer();

        // Assign snake and food stuff
        int dir = ran.nextInt(2);         //  0 or 1
        int mag = ran.nextInt(2) * 2 - 1; // -1 or 1
        int raX = ran.nextInt(11) + 15;   // 15 to 25
        int raY = (ran.nextInt(11) + 10); // 10 to 20
        snake      = new Snake(dir, mag, raX, raY);
        snake.tail = new ArrayList<Integer>();
        snake.tail.add(snake.x);
        snake.tail.add(snake.y);
        snake.justAte = false;
        ateTail = false;

        food   = new Food();
        food.x = food.getLocation(ran.nextInt(40));  // The screen has 40 cols
        food.y = food.getLocation(ran.nextInt(30));  // and 30 rows.

        buttonPressed = false;  // This is to avoid clicking twice before the counter to move is up,
        // making it impossible to turn the snake back on itself. It is set true upon click and reset
        // in the snake's move function.

        // Assign AI stuff
        aiMode = false;
        snakeAI = new SnakeAI();
        moveArray = new ArrayList<Integer>();
        move = 0;

        // Read or create the config file
        boolean fileExists = new File("snake.conf").isFile();
        if (!fileExists) {
            try {
                PrintWriter pr = new PrintWriter("snake.conf", "UTF-8");
                pr.print("0 ");
                pr.print("100 ");
                pr.print("12 ");
                pr.print("false");
                pr.close();
            }
            catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
            hs = 0; delay = 100; fontSize = 12; showFPS = false;  // The file can't be read if it doesn't exist...
        }
        else {
            try {
                Scanner fs = new Scanner(new File("snake.conf"));
                String[] line = fs.nextLine().split(" ");
                hs = Integer.parseInt(line[0]);
                delay = Integer.parseInt(line[1]);
                fontSize = Integer.parseInt(line[2]);
                showFPS = Boolean.parseBoolean(line[3]);

                fs.close();
            }
            catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
            catch (NumberFormatException e) {
                System.err.println("NumberFormatException: " + e.getMessage());
            }
        }

        // Stage
        ftfg = new FreeTypeFontGenerator(Gdx.files.internal("Hack-Bold.ttf"));
        FreeTypeFontParameter ftfp = new FreeTypeFontParameter();
        ftfp.size = fontSize;
        BitmapFont font = ftfg.generateFont(ftfp);
        scoreLabel = new Label("Send Nudes", new Label.LabelStyle(font, Color.GRAY));
        strB       = new StringBuilder();
        stage      = new Stage();
        updateScore();
        stage.addActor(scoreLabel);

        // Start the chronometer
        startTime = System.currentTimeMillis();
    }

    @Override
    public void render() {
        Gdx.graphics.setWindowedMode(640, 480);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.draw();

        // Render the snake
        for (int i = 0; i < snake.tail.size(); i += 2) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.rect(snake.tail.get(i), snake.tail.get(i + 1), Snake.side, Snake.side);
            shapeRenderer.end();
        }

        // Render the food
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(food.x, food.y, Food.side, Food.side);
        shapeRenderer.end();

        // Check user input
        if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT) && snake.moveX == 0 && !buttonPressed && !aiMode) {
            snake.moveX = - Snake.side;
            snake.moveY = 0;
            buttonPressed = true;
        }
        else if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT) && snake.moveX == 0 && !buttonPressed && !aiMode) {
            snake.moveX = Snake.side;
            snake.moveY = 0;
            buttonPressed = true;
        }
        else if (Gdx.input.isKeyPressed(Keys.DPAD_UP) && snake.moveY == 0 && !buttonPressed && !aiMode) {
            snake.moveX = 0;
            snake.moveY = Snake.side;
            buttonPressed = true;
        }
        else if (Gdx.input.isKeyPressed(Keys.DPAD_DOWN) && snake.moveY == 0 && !buttonPressed && !aiMode) {
            snake.moveX = 0;
            snake.moveY = - Snake.side;
            buttonPressed = true;
        }
        else if (Gdx.input.isKeyPressed(Keys.A)) {
            aiMode = !aiMode;  // the A key toggles aiMode
        }



        // Move at 10 fps for the ultimate gaming experience
        if (System.currentTimeMillis() -  startTime >= delay) {

            if (aiMode) {
                if (moveArray.size() < 1) {
                    moveArray = snakeAI.getMoves(food.x, food.y, snake.tail);
                }
                move = moveArray.get(0);
                if (move == 0 && snake.moveX == 0) {
                    snake.moveX = Snake.side;
                    snake.moveY = 0;
                }
                else if (move == 1 && snake.moveY == 0) {
                    snake.moveX = 0;
                    snake.moveY = Snake.side;
                }
                else if (move == 2 && snake.moveX == 0) {
                    snake.moveX = - Snake.side;
                    snake.moveY = 0;
                }
                else if (move == 3 && snake.moveY == 0) {
                    snake.moveX = 0;
                    snake.moveY = - Snake.side;
                }
                moveArray.remove(0);
            }

            snake.move();

            // Check out of bounds and move accordingly
            if (snake.x >= 640) {
                snake.goBack(); // Back to previous position,
                snake.x = -16;  // then set it in the right place,
                snake.move();  // then move.
            }
            else if (snake.x < -1) {
                snake.goBack();
                snake.x = 640;
                snake.move();
            }
            else if (snake.y >= 480) {
                snake.goBack();
                snake.y = -16;
                snake.move();
            }
            else if (snake.y < 0) {
                snake.goBack();
                snake.y = 480;
                snake.move();
            }
            buttonPressed = false;

            startTime = System.currentTimeMillis();

            // Eating the food
            if (snake.x == food.x && snake.y == food.y) {
                snake.justAte = true; // This only has effect in the next move.
                food.x = food.getLocation(ran.nextInt(40));
                food.y = food.getLocation(ran.nextInt(30));
            }
            updateScore();


            // Eating the tail
            ateTail = snake.checkAteTail();
            if (ateTail) {
                if ((snake.tail.size() / 2) - 1 > hs) {
                    hs = (snake.tail.size() / 2) - 1;
                    try {
                        PrintWriter pr = new PrintWriter("snake.conf", "UTF-8");
                        pr.print(hs + " " + delay + " " + fontSize + " " + showFPS);
                        pr.close();
                    }
                    catch (IOException e) {
                        System.err.println("IOException: " + e.getMessage());
                    }
                }
                snake.restart();
            }

        } // End of the delay

    }

    private void updateScore() {
        strB.setLength(0);
        strB.append("High Score: ").append(hs);
        strB.append("  |  Current: ").append((snake.tail.size() / 2) - 1);
        if (showFPS) {
            strB.append("  FPS: ").append(Gdx.graphics.getFramesPerSecond());
        }
        if (aiMode) {
            strB.append("  AI ON");
        }
        scoreLabel.setText(strB);
    }

    @Override
    public void resize(int x, int y) {

    }

    @Override
    public void dispose () {
        shapeRenderer.dispose();
        stage.dispose();
        ftfg.dispose();
    }
}
