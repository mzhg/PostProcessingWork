package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class Group implements Constant, OnTouchEventListener, AnimationEventListener{
    private static final int STATE_FOCUS = 1 << 0;
    private static final int STATE_FILM = 1 << 1;
    private static final int STATE_FILM_START_ANIMATION = 1 << 2;
    private static final int STATE_FILM_END_ANIMATION = 1 << 3;

    final int groupID;
    final AppIcon[] icons = new AppIcon[MAX_NUM_ICONS_PER_GROUP];
    final Transform screenLocation = new Transform();
    OSRenderer renderer;

    private Drawable m_FilmDrawable;
    private int m_FilmAppIndex = -1;
    private final AppList m_AppList = new AppList(this);

    private Drawable m_WatchedDrawable;
    private Drawable m_HoveredDrawable;
    private Drawable m_CloseDrawable;
    private final Transform m_CloseTransform = new Transform();
    private  int m_States;
    private SceneRaycastSelector m_RaycastSelector;
    private final List<Drawable> m_Drawables = new ArrayList<>();
    private final Matrix4f m_Model = new Matrix4f();
    private float          m_WatchingTime;

    Group(OSRenderer renderer, int groupID, Drawable closeDrawable){
        this.renderer = renderer;
        this.groupID = groupID;

        m_RaycastSelector = new SceneRaycastSelector(this, renderer.m_ViewRay);
        m_RaycastSelector.setWatchingThreshold(1.5f);

        m_CloseDrawable = closeDrawable;
    }

    void setAppIcon(int index, AppIcon icon) { icons[index] = icon;}
    AppIcon get(int index) { return icons[index];}
    AppList  getAppList()  { return m_AppList;}

    void setFocused(boolean focused){
//        if(renderer.m_State == OSRenderer.STATE_FILE && renderer.m_Film.m_FilmGroup == groupID )
//            return;

        if(isFocused() != focused){
            if(!isFilmState()) {
                for (AppIcon icon : icons) {
                    Drawable drawable = icon.drawable;
                    Vector3f destPos;
                    Quaternion destRotation;
                    if (focused) {
                        // expand the icons
                        destPos = icon.worldLocation.translate;
                        destRotation = icon.worldLocation.rotation;
                    } else {
                        destPos = icon.unfocusedLocation.translate;
                        destRotation = icon.unfocusedLocation.rotation;
                    }

                    drawable.addRotationAnimation(destRotation, 1.0f);
                    drawable.addTraslationAnimation(destPos.x, destPos.y, destPos.z, 1.0f);
                }
            }

            setState(focused, STATE_FOCUS);
        }
    }

    void update(float dt){
        if(m_Drawables.isEmpty()){
            for(AppIcon appIcon : icons){
                m_Drawables.add(appIcon.drawable);
            }
        }

        m_RaycastSelector.update(m_Drawables, dt);
        for(AppIcon icon : icons){
            icon.drawable.update(dt);
        }

        if(m_FilmDrawable != null){
            m_FilmDrawable.update(dt);
        }
    }

    void renderer(Matrix4f viewProj){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);

        for(AppIcon appIcon : icons){
            if(!appIcon.drawable.isVisible() /*|| (m_WatchedDrawable != null && m_WatchedDrawable == appIcon.drawable)*/){
                continue;
            }

            renderDrawable(appIcon.drawable, viewProj);
        }

        if(is(STATE_FILM) || is(STATE_FILM_START_ANIMATION) || is(STATE_FILM_END_ANIMATION)){
            renderDrawable(m_FilmDrawable, viewProj);
        }

        if(isFilmState()){
            m_CloseDrawable.transform.set(m_CloseTransform);
            m_CloseDrawable.setVisible(true);
            renderDrawable(m_CloseDrawable, viewProj);
        }
    }

    void renderDrawable(Drawable drawable, Matrix4f viewProj){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        SingleTextureProgram program = (SingleTextureProgram)drawable.program;
        VertexArrayObject rectVAO = drawable.buffer;

        rectVAO.bind();
        drawable.transform.toMatrix(m_Model);
        Matrix4f mvp = Matrix4f.mul(viewProj, m_Model, m_Model);
        program.setMVP(mvp);
        if(m_HoveredDrawable == drawable){
            program.setHovered(true);
        }

        if(m_WatchedDrawable == drawable){
            program.setRenderCircle(true);
            program.setCircleTime(m_WatchingTime);
        }

        if(drawable == m_FilmDrawable){
            program.setRenderFilm(true);
        }

        gl.glBindTexture(drawable.texture.getTarget(), drawable.texture.getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        if(m_HoveredDrawable == drawable){
            program.setHovered(false);
        }

        if(m_WatchedDrawable == drawable){
            program.setRenderCircle(false);  // disable the circle
        }
        if(drawable == m_FilmDrawable){
            program.setRenderFilm(false);
        }


        rectVAO.unbind();
    }

    private void setState(boolean enabled, int bit){
        if(enabled){
            m_States |= bit;
            m_RaycastSelector.enable();
        }else{
            m_States &= (~bit);
            m_RaycastSelector.disable();
        }
    }

    boolean isFocused() { return (m_States & STATE_FOCUS) != 0;}
    boolean isFilmState() { return (m_States & STATE_FILM) != 0;}
    private boolean is(int bit) { return (m_States & bit) != 0;}

    private void addAnimation(Drawable drawable, Transform dest){
        drawable.addTraslationAnimation(dest.translate.x, dest.translate.y, dest.translate.z, 1.0f);
        drawable.addScaleAnimation(dest.scale.x, dest.scale.y, dest.scale.z, 1.0f);
        drawable.addRotationAnimation(dest.rotation, 1.0f);
    }

    private void setFilmState(boolean filmState){
//        if(isFilmState() != filmState){
            if(filmState){
                if(m_FilmDrawable == null) {
                    m_FilmDrawable = new Drawable();
                    m_FilmDrawable.addAnimationEventListener(this);
                }

                // prepare for the start animation
                {
                    final int prevFilmAppIndex = m_FilmAppIndex;

                    { // make the selected drawable to film drawable
                        Drawable drawable = m_WatchedDrawable;
                        m_FilmAppIndex = findDrawableIndex(drawable);
                        AppIcon appIcon = icons[m_FilmAppIndex];
                        // If the watched drawable that already in the app list, remove it first
                        m_AppList.remove(appIcon);

                        m_FilmDrawable.transform.set(drawable.transform);
                        m_FilmDrawable.texture = drawable.texture;
                        m_FilmDrawable.buffer = drawable.buffer;
                        m_FilmDrawable.program = drawable.program;

                        addAnimation(m_FilmDrawable, screenLocation);

                        setState(true, STATE_FILM_START_ANIMATION);
                        icons[m_FilmAppIndex].drawable.setVisible(false);
                    }

                    boolean needToReadyLocation = true;
                    // If we had film-drawable before, we need push it to the app list.
                    if(prevFilmAppIndex != -1){
                        AppIcon appIcon = icons[prevFilmAppIndex];
                        appIcon.drawable.transform.set(screenLocation);  // set the transform to the screenLocation for the next animation
                        m_AppList.add(appIcon);

                        needToReadyLocation = false;
                    }

                    if(needToReadyLocation) {
                        for (AppIcon _appIcon : icons) {
                            if (_appIcon.drawable != m_WatchedDrawable ) {
                                Transform dest = _appIcon.readyLocation;
                                _appIcon.drawable.addTraslationAnimation(dest.translate.x, dest.translate.y, dest.translate.z, 1.0f);
                                _appIcon.drawable.addScaleAnimation(dest.scale.x, dest.scale.y, dest.scale.z, 1.0f);
                                _appIcon.drawable.addRotationAnimation(dest.rotation, 1.0f);
                            }
                        }
                    }
                }
                // go back to the origin position.
            }else{
                Drawable drawable = icons[m_FilmAppIndex].drawable;
                Transform dest = drawable.transform;

                m_FilmDrawable.transform.set(drawable.transform);
                m_FilmDrawable.texture = drawable.texture;
                m_FilmDrawable.buffer = drawable.buffer;
                m_FilmDrawable.program = drawable.program;

                m_FilmDrawable.addAnimationEventListener(this);
                m_FilmDrawable.addTraslationAnimation(dest.translate.x, dest.translate.y, dest.translate.z, 1.0f);
                m_FilmDrawable.addScaleAnimation(dest.scale.x, dest.scale.y, dest.scale.z, 1.0f);

                setState(false, STATE_FILM);
                setState(true, STATE_FILM_END_ANIMATION);
                icons[m_FilmAppIndex].drawable.setVisible(true);
            }
//        }
    }

    @Override
    public void onFinished(int id) {
        if(is(STATE_FILM_START_ANIMATION)){
            setState(false, STATE_FILM_START_ANIMATION);  // clear the start animation flag
            setState(true, STATE_FILM);

            // TODO
            final float scale_value = 0.15f;
            Transform dest = m_CloseTransform;
            dest.set(m_FilmDrawable.transform);
            dest.scale.scale(scale_value);

            // Calculate the direction from the center to righttop
            m_FilmDrawable.transform.toMatrix(m_Model);
            Vector3f rightTop = new Vector3f(-1,1,-0.1f);
            Matrix4f.transformVector(m_Model, rightTop, rightTop);
            Vector3f dir = Vector3f.sub(rightTop, m_FilmDrawable.transform.translate, rightTop);
            float length = dir.length();
            dir.scale(1.0f/length);

            dest.translate.y += dir.y * length * (1-scale_value*1.f);
            dest.translate.x += dir.x * length * (1-scale_value*1.f);
            dest.translate.z += dir.z * length * (1-scale_value*1.f);

        }else if(is(STATE_FILM_END_ANIMATION)){
            setState(false, STATE_FILM_END_ANIMATION);
        }
    }

    public int findDrawableIndex(Drawable drawable){
        for(int i = 0; i < icons.length; i++){
            if(icons[i].drawable == drawable){
                return i;
            }
        }

        return -1;
    }

    @Override
    public void OnEnter(int id, Drawable drawable) {
        renderer.m_Tocuhed = true;
        m_HoveredDrawable = drawable;
    }

    @Override
    public void OnLeval(int id, Drawable drawable) {
        renderer.m_Tocuhed = false;
        m_HoveredDrawable = null;
        m_WatchedDrawable = null;
    }

    @Override
    public void OnWatching(int id, Drawable drawable, float time) {
        m_WatchedDrawable = drawable;
        m_WatchingTime = (time - m_RaycastSelector.getWatchingThreshold()) / 4.0f;
        int idx = findDrawableIndex(drawable);
        AppIcon appIcon = icons[idx];

        if (m_WatchingTime > 1.f) {
            setFilmState(true);
        }

//        if(isFilmState()){
//            setFilmState(false);
//        }else {
//
//        }
    }
}
