package org.ems;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.simsilica.lemur.*;
import com.simsilica.lemur.style.BaseStyles;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientWindow extends SimpleApplication {
    interface Observer{
        void onObservableChanged(String command);
    }
    private boolean isRunning = true;
    protected Geometry player;
    protected Geometry opponent;
    protected Geometry plane;
    private final Set<Client.Observer> mObservers = Collections.newSetFromMap(new ConcurrentHashMap<Client.Observer, Boolean>(0));
    private String intendedDirection; //This one might be obsolete.
    public String map;
    public int playerNumber;
    private Node walls;
    
    public ClientWindow(String map, int playerNumber) {
        super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false),
                new ScreenshotAppState("", System.currentTimeMillis()));
        setShowSettings(false);
        setDisplayFps(true);
        setDisplayStatView(false);
        setPauseOnLostFocus(false);
        this.intendedDirection = "";
        this.map = map;
        this.playerNumber = playerNumber;
    }
    
    public void registerObserver(Client.Observer observer){
        if (observer==null) return;
        mObservers.add(observer);
    }
    
    public void unregisterObserver(Client.Observer observer){
        if(observer!=null) mObservers.remove(observer);
    }
    
    private void notifyObservers(String playerCommand){
        for(Client.Observer o : mObservers){
            o.onObservableChanged(playerCommand);
        }
    }
        
    @Override
    public void simpleInitApp() {
        Quad p = new Quad(30,30,false); //Floor
        plane = new Geometry("Plane", p); //Floor
        plane.setLocalTranslation(-15f,-15f,-0.75f);
        cam.setLocation(new Vector3f(5.0f,-5.0f,14.0f));

        Material boxMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"); //Boxes and floor.
        boxMat.setBoolean("UseMaterialColors",true);
        boxMat.setColor("Diffuse", ColorRGBA.LightGray);
        boxMat.setColor("Ambient", ColorRGBA.LightGray );
        boxMat.setFloat("Shininess", 0.4f);
        boxMat.setColor("Specular",ColorRGBA.White);
        boxMat.setColor("GlowColor",ColorRGBA.Cyan); //Glow color on material (off)
        
        plane.setMaterial(boxMat); //floor

        SpotLight spot = new SpotLight(); //Light
        spot.setSpotRange(100f);                           // distance
        spot.setSpotInnerAngle(20f * FastMath.DEG_TO_RAD); // inner light cone (central beam)
        spot.setSpotOuterAngle(45f * FastMath.DEG_TO_RAD); // outer light cone (edge of the light)
        spot.setColor(ColorRGBA.White.mult(1.4f));         // light color
        spot.setPosition(new Vector3f(3.0f,2.0f,11.0f));               // shine from camera loc
        Vector3f temp = cam.getDirection();
        Vector3f temp2 = new Vector3f(temp.x+0.25f, temp.y-0.5f, temp.z);
        
        spot.setDirection(temp2);              // shine forward from camera loc
        rootNode.addLight(spot);

        plane.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        final int SHADOWMAP_SIZE=1024;
        SpotLightShadowRenderer slsr = new SpotLightShadowRenderer(assetManager, SHADOWMAP_SIZE);
        slsr.setLight(spot);
        slsr.setShadowIntensity(0.6f);
        viewPort.addProcessor(slsr);
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        SSAOFilter ssaoFilter = new SSAOFilter(1.1f, .5f, 0.02f, 0.01f);
        fpp.addFilter(ssaoFilter);
        viewPort.addProcessor(fpp);
        
        showMap();
        
        rootNode.attachChild(plane);
        
        initKeys(); // Load my custom keybinding.
                
        //GUI
        // Initialize the globals access so that the default
        // components can find what they need.
        GuiGlobals.initialize(this);
        // Load the 'glass' style
        BaseStyles.loadGlassStyle();
        // Set 'glass' as the default style when not specified
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        Container myUiElements = new Container();
        myUiElements.setName("myUiElements");
        guiNode.attachChild(myUiElements);
        myUiElements.setLocalTranslation(0, 480, 0);
        //ElementId if Label is by default 'label'.
        Label myLabel = new Label("Status: Waiting for the other player.");
        myLabel.setName("myLabel");
        myUiElements.addChild(myLabel);
        Button button = new Button("Disconnect");
        button.setLocalTranslation(0,450, 0);
        guiNode.attachChild(button);
        button.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                notifyObservers("quit");
            }
        });
    }
    protected Geometry makeWallBox(String name, float x, float y, float z, ColorRGBA color) {
        Box box = new Box(0.5f, 0.5f, 0.5f);
        Geometry boxBox = new Geometry(name, box);
        boxBox.setLocalTranslation(x, y, z);
        Material mat1 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat1.setBoolean("UseMaterialColors",true);
        mat1.setColor("Diffuse", color);
        mat1.setColor("Ambient", ColorRGBA.fromRGBA255(34,76,103, 256) );
        mat1.setFloat("Shininess", 0.6f);
        mat1.setColor("Specular",ColorRGBA.White);
        boxBox.setMaterial(mat1);
        //boxBox.setShadowMode(RenderQueue.ShadowMode.CastAndReceive); //can this go here?
        return boxBox;
    }
    private void showMap() {
        walls = new Node("walls");
        rootNode.attachChild(walls);
        walls.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        int ROWS = 10, COLUMNS=10;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (map.regionMatches(((row * COLUMNS) + column), "#", 0, 1)) { //Walls.
                    walls.attachChild(makeWallBox("row"+row+"col"+column, (float)column, -(float)row, 0f, ColorRGBA.LightGray ));
                }
                if (map.regionMatches(((row * COLUMNS) + column), "b", 0, 1)) { //Key 1.
                    walls.attachChild(makeWallBox("key1", (float)column, -(float)row, 0f, ColorRGBA.Pink));
                }
                if (map.regionMatches(((row * COLUMNS) + column), "c", 0, 1)) { //Key 2.
                    walls.attachChild(makeWallBox("key2", (float)column, -(float)row, 0f, ColorRGBA.Yellow ));
                }
                if (map.regionMatches(((row * COLUMNS) + column), "o", 0, 1)) { //Door 1.
                    walls.attachChild(makeWallBox("door1", (float)column, -(float)row, 0f, ColorRGBA.Orange ));
                }
                if (map.regionMatches(((row * COLUMNS) + column), "p", 0, 1)) { //Door 2.
                    walls.attachChild(makeWallBox("door2", (float)column, -(float)row, 0f, ColorRGBA.Cyan ));
                }
                if (map.regionMatches(((row * COLUMNS) + column), "1", 0, 1)) { //Player 1.
                    if (playerNumber == 1) {
                        Box b1 = new Box(0.5f, 0.5f, 0.5f);
                        player = new Geometry("Player1", b1);
                        player.setLocalTranslation((float) column, -(float) row, 0);
                        Material pl1Mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"); //Player
                        pl1Mat.setBoolean("UseMaterialColors", true);
                        pl1Mat.setColor("Diffuse", ColorRGBA.Red);
                        pl1Mat.setColor("Ambient", ColorRGBA.fromRGBA255(34, 76, 103, 256));
                        pl1Mat.setFloat("Shininess", 0.6f);
                        pl1Mat.setColor("Specular", ColorRGBA.White);
                        pl1Mat.setColor("GlowColor", ColorRGBA.Cyan); //Glow color on material (off)
                        player.setMaterial(pl1Mat);
                        player.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                        walls.attachChild(player);
                    }
                    if (playerNumber == 2) {
                        Box b2 = new Box(0.5f, 0.5f, 0.5f);
                        opponent = new Geometry("Opponent2", b2);
                        opponent.setLocalTranslation((float) column, -(float) row, 0);
                        Material opp1Mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        opp1Mat.setBoolean("UseMaterialColors", true);
                        opp1Mat.setColor("Diffuse", ColorRGBA.Red);
                        opp1Mat.setColor("Ambient", ColorRGBA.fromRGBA255(34, 76, 103, 256));
                        opp1Mat.setFloat("Shininess", 0.6f);
                        opp1Mat.setColor("Specular", ColorRGBA.White);
                        opp1Mat.setColor("GlowColor", ColorRGBA.Cyan); //Glow color on material (off)
                        opponent.setMaterial(opp1Mat);
                        opponent.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                        walls.attachChild(opponent);
                    }
                }
                if (map.regionMatches(((row * COLUMNS) + column), "2", 0, 1)) { //Player 1.
                    if (playerNumber == 2) {
                        Box b3 = new Box(0.5f, 0.5f, 0.5f);
                        player = new Geometry("Player2", b3);
                        player.setLocalTranslation((float) column, -(float) row, 0);
                        Material pl2Mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"); //Player
                        pl2Mat.setBoolean("UseMaterialColors", true);
                        pl2Mat.setColor("Diffuse", ColorRGBA.Green);
                        pl2Mat.setColor("Ambient", ColorRGBA.fromRGBA255(34, 76, 103, 256));
                        pl2Mat.setFloat("Shininess", 0.6f);
                        pl2Mat.setColor("Specular", ColorRGBA.White);
                        pl2Mat.setColor("GlowColor", ColorRGBA.Cyan); //Glow color on material (off)
                        player.setMaterial(pl2Mat);
                        player.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                        walls.attachChild(player);
                    }
                    if (playerNumber == 1) {
                        Box b4 = new Box(0.5f, 0.5f, 0.5f);
                        opponent = new Geometry("Opponent1", b4);
                        opponent.setLocalTranslation((float) column, -(float) row, 0);
                        Material opp2Mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        opp2Mat.setBoolean("UseMaterialColors", true);
                        opp2Mat.setColor("Diffuse", ColorRGBA.Green);
                        opp2Mat.setColor("Ambient", ColorRGBA.fromRGBA255(34, 76, 103, 256));
                        opp2Mat.setFloat("Shininess", 0.6f);
                        opp2Mat.setColor("Specular", ColorRGBA.White);
                        opp2Mat.setColor("GlowColor", ColorRGBA.Cyan); //Glow color on material (off)
                        opponent.setMaterial(opp2Mat);
                        opponent.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
                        walls.attachChild(opponent);
                    }
                }
            }
        }
    }

    private void initKeys() {
        // We can map one or several inputs to one named action...
        inputManager.addMapping("Up",  new KeyTrigger(KeyInput.KEY_W),
                                                    new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Left",   new KeyTrigger(KeyInput.KEY_A),
                                                        new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right",  new KeyTrigger(KeyInput.KEY_D),
                                                        new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S),
                new KeyTrigger(KeyInput.KEY_DOWN));
        // Add the names to the action listener.
        inputManager.addListener(actionListener, "Up", "Left", "Right", "Down");
        // inputManager.addListener(analogListener, "Up", "Left", "Right", "Down");
    }
    
    private final ActionListener actionListener = new ActionListener() {
        private String intendedDirection=ClientWindow.this.intendedDirection; //Probably obsolete.

        @Override
        public void onAction(String name, boolean keyPressed, float tfp) {
            if (name.equals("Up") && !keyPressed ) {
                notifyObservers("up");
            }
            if (name.equals("Down")&& !keyPressed) {
                notifyObservers("down");
            }
            if (name.equals("Right")&& !keyPressed) {
                notifyObservers("right");
            }
            if (name.equals("Left")&& !keyPressed) {
                notifyObservers("left");
            }
        }
    };
    
    public void moveOpponent(String command) { //Perhaps not used.
        System.out.println("Reached the moveOpponent.");
        float value=1f;//arbitrary offset for now.
        Vector3f v = opponent.getLocalTranslation();
        if(command.startsWith("up")){ //consider cleaning up the input stings at the source so that I don't have to do startsWith(), but can use equals().
            opponent.setLocalTranslation(v.x, v.y + value, v.z);
        }
        if(command.startsWith("down")){
            opponent.setLocalTranslation(v.x, v.y - value, v.z);
        }
        if(command.startsWith("right")){
            opponent.setLocalTranslation(v.x + value, v.y, v.z);
        }
        if(command.startsWith("left")){
            opponent.setLocalTranslation(v.x - value, v.y, v.z);
        }
    }

//    private final AnalogListener analogListener = new AnalogListener() {
//        @Override
//        public void onAnalog(String name, float value, float tpf) {
//            float valuetemp=0.4f;
//            if (isRunning) {
//                if (name.equals("Up")) {
//                    Vector3f v = player.getLocalTranslation();
//                    player.setLocalTranslation(v.x, v.y + valuetemp * speed, v.z);
//                }
//                if (name.equals("Down")) {
//                    Vector3f v = player.getLocalTranslation();
//                    player.setLocalTranslation(v.x, v.y - valuetemp * speed, v.z);
//                }
//                if (name.equals("Right")) {
//                    Vector3f v = player.getLocalTranslation();
//                    player.setLocalTranslation(v.x + valuetemp * speed, v.y, v.z);
//                }
//                if (name.equals("Left")) {
//                    Vector3f v = player.getLocalTranslation();
//                    player.setLocalTranslation(v.x - valuetemp * speed, v.y, v.z);
//                }
//            } 
//        }
//    };

    @Override
    public void simpleUpdate(float tpf) {
        //Add update code if needed.
    }
}
