package jet.opengl.demos.gpupro.clustered;

import jet.opengl.postprocessing.util.LogUtil;

final class SceneManager {
    //String could probably be an enum instead, but it's easier this way to build
    //the relative paths if it is a string.
    private String currentSceneID;
    private Scene currentScene;

    //Initializes and closes all scene related stuff
    boolean startUp(){
        // currentSceneID = "pbrTest";
        currentSceneID = "Sponza";
        if (!loadScene(currentSceneID)){
            LogUtil.i(LogUtil.LogType.DEFAULT, "Could not load default sponza scene. No models succesfully loaded!\n");
            return false;
        }
        return true;
    }
    void shutDown(){ }

    // Scene switching
    boolean switchScene(String sceneID){  return true;}

    // Update current scene
    void update( int deltaT){
        currentScene.update(deltaT);
    }

    //Called by the rendermanager to prep the render queue
    Scene getCurrentScene(){ return currentScene;}

    private boolean loadScene(String sceneID){
        currentScene = new Scene(sceneID);
        return  !currentScene.loadingError; //True if empty, so it's negated for startup
    }
}
