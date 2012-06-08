import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Group;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Screen3D;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnBehaviorPost;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGEncodeParam;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.universe.SimpleUniverse;


public class PathView implements NewBlockListener {
	SimpleUniverse u;
	Canvas3D canvas;
	JFrame frame;
	LineStripArray line;
	private AddLineBehavior addLineBehavior;
	private LinkedList<Integer[]> newBlocks= new LinkedList<Integer[]>();
	private Point3d lastPoint= new Point3d();
	private BoundingBox bounds= new BoundingBox(new Point3d(), new Point3d());
	private Point3d upperBound= new Point3d();
	private Point3d lowerBound= new Point3d();
			
	// colors for use in the cones
	Color3f red = new Color3f(1.0f, 0.0f, 0.0f);
	Color3f black = new Color3f(0.0f, 0.0f, 0.0f);
	Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

	// geometric constants
	Point3f origin = new Point3f();
	Vector3f yAxis = new Vector3f(0.0f, 1.0f, 0.0f);

	// NumberFormat to print out floats with only two digits
	NumberFormat nf;

	
	public synchronized void newBlock(Integer[] steps) {
    	System.out.println("newBlock");

		newBlocks.addLast(steps);
		addLineBehavior.postId(1);
	}
	
	protected synchronized Integer[] getNextStep() {
		if(newBlocks.size()>0)
			return newBlocks.removeFirst();
		else
			return null;
	}
	
	// Returns the TransformGroup we will be editing to change the tranform
	// on the lines
	private Group createLineTypes() {

		Group lineGroup = new Group();

		Appearance app = new Appearance();
		ColoringAttributes ca = new ColoringAttributes(white, ColoringAttributes.SHADE_FLAT);
		app.setColoringAttributes(ca);

		line = new LineStripArray(100000, LineArray.COORDINATES, new int[] {2});
		line.setCapability(LineStripArray.ALLOW_COORDINATE_WRITE);
		line.setCapability(LineStripArray.ALLOW_COUNT_WRITE);
		
		line.setCoordinate(0, new float[]{0.0f, 0.0f, 0.0f});
		line.setCoordinate(1, new float[]{0.0f, 0.0f, 0.0f});
		Shape3D sqShape = new Shape3D(line, app);
		sqShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		lineGroup.addChild(sqShape);
		
		return lineGroup;
	}
	
    public class AddLineBehavior extends Behavior {
        private WakeupCriterion postCondition;   
   
        AddLineBehavior() {   
        	postCondition= new WakeupOnBehaviorPost(null, 0);
//        	postCondition= new WakeupOnElapsedTime(500);
        }   
   
        public void initialize(){   
        	System.out.println("initialize");
            wakeupOn(postCondition);   
        }   
       
        public void processStimulus(Enumeration criteria) {
        	System.out.println("processStimulus");
        	Integer[] steps;
        	while((steps= getNextStep())!=null)
        		addBlock(steps);
            wakeupOn(postCondition);   
        }
        
        private void addBlock(Integer[] steps) {
    		int nextIdx= line.getValidVertexCount();
    		System.out.println("nextIdx: " + nextIdx);
    		lastPoint.x+= steps[0].floatValue();
    		lastPoint.y+= steps[1].floatValue();
    		lastPoint.z+= steps[2].floatValue();
    		bounds.combine(lastPoint);
    		
    		line.setCoordinate(nextIdx, lastPoint);
    		line.setStripVertexCounts(new int[]{nextIdx+1});
    		zoomToFit();
        }
    }
   
	private BranchGroup createSceneGraph() {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();

		addLineBehavior= new AddLineBehavior();
        // set scheduling bounds for behavior objects   
        BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 10000);   
        addLineBehavior.setSchedulingBounds(bounds);   

		objRoot.addChild(addLineBehavior);

		// Create a TransformGroup to scale the scene down by 3.5x
		// TODO: move view platform instead of scene using orbit behavior
//		TransformGroup objScale = new TransformGroup();
//		Transform3D scaleTrans = new Transform3D();
//		// scaleTrans.set(1 / 3.5f); // scale down by 3.5x
//		objScale.setTransform(scaleTrans);
//		objRoot.addChild(objScale);
//
//		// Create a TransformGroup and initialize it to the
//		// identity. Enable the TRANSFORM_WRITE capability so that
//		// the mouse behaviors code can modify it at runtime. Add it to the
//		// root of the subgraph.
//		TransformGroup objTrans = new TransformGroup();
//		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
//		objScale.addChild(objTrans);

		// Add the primitives to the scene
//		objTrans.addChild(createLineTypes());
		objRoot.addChild(createLineTypes());
		
//		BoundingSphere bounds = new BoundingSphere(new Point3d(), 10000.0);
//		Background bg = new Background(new Color3f(1.0f, 1.0f, 1.0f));
//		bg.setApplicationBounds(bounds);
//		objTrans.addChild(bg);
//
//		// set up the mouse rotation behavior
//		MouseRotate mr = new MouseRotate();
//		mr.setTransformGroup(objTrans);
//		mr.setSchedulingBounds(bounds);
//		mr.setFactor(0.007);
//		objTrans.addChild(mr);
//
//		// Set up the ambient light
//		Color3f ambientColor = new Color3f(0.1f, 0.1f, 0.1f);
//		AmbientLight ambientLightNode = new AmbientLight(ambientColor);
//		ambientLightNode.setInfluencingBounds(bounds);
//		objRoot.addChild(ambientLightNode);
//
//		// Set up the directional lights
//		Color3f light1Color = new Color3f(1.0f, 1.0f, 1.0f);
//		Vector3f light1Direction = new Vector3f(0.0f, -0.2f, -1.0f);
//
//		DirectionalLight light1 = new DirectionalLight(light1Color,
//				light1Direction);
//		light1.setInfluencingBounds(bounds);
//		objRoot.addChild(light1);
//
		objRoot.compile();
		
		return objRoot;
	}

	public void init() {
		frame= new JFrame("Tool path view");
		frame.setBounds(500, 50, 300, 300);
		// set up a NumFormat object to print out float with only 3 fraction
		// digits
		frame.setLayout(new BorderLayout());
		
		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

		canvas = new Canvas3D(config);

		frame.getContentPane().add("Center", canvas);

		// Create a simple scene and attach it to the virtual universe
		u = new SimpleUniverse(canvas);

		// This will move the ViewPlatform back a bit so the
		// objects in the scene can be viewed.
//		u.getViewingPlatform().setNominalViewingTransform();
		TransformGroup vt= u.getViewingPlatform().getViewPlatformTransform();		
		Transform3D t3d = new Transform3D();
		vt.getTransform(t3d);
		t3d.lookAt(new Point3d(0,0,1), new Point3d(0,0,0), new Vector3d(0,1,0));
		t3d.invert();
		vt.setTransform(t3d);
		
		BranchGroup scene = createSceneGraph();
		u.addBranchGraph(scene);
		
//		OrbitBehavior orbit= new OrbitBehavior(canvas);
//		orbit.setSchedulingBounds(new BoundingSphere(new Point3d(0, 0, 0), 1000));
//		u.getViewingPlatform().setViewPlatformBehavior(orbit);
		
//		System.out.println("getViewPlatform().getActivationRadius: " + u.getViewingPlatform().getViewPlatform().getActivationRadius());		
	}

	private void zoomToFit() {
		bounds.getUpper(upperBound);
		bounds.getLower(lowerBound);
		double extentX= upperBound.x-lowerBound.x;
		if(extentX<=0) return;
		
		double centerX= 0.5*(upperBound.x+lowerBound.x);
		double centerY= 0.5*(upperBound.y+lowerBound.y);
		double centerZ= 0.5*(upperBound.z+lowerBound.z);
		double extentZ= upperBound.z-lowerBound.z;
		Point3d center= new Point3d(centerX,centerY,centerZ);
//		System.out.println("center: "+ center);
		Point3d eyeOffset= new Point3d(0,0,1);
		double angleOfView= canvas.getView().getFieldOfView();
		double distance= (1.03*extentX/2)/Math.tan(angleOfView/2);
		eyeOffset.scale(distance);
		eyeOffset.add(center);
//		System.out.println("angleOfView: " + angleOfView + ", distance: " + distance + ", eyeOffset: "+ eyeOffset);
		
		
		TransformGroup vt= u.getViewingPlatform().getViewPlatformTransform();		
		Transform3D t3d = new Transform3D();
		vt.getTransform(t3d);
		t3d.lookAt(eyeOffset, center, new Vector3d(0,1,0));
		t3d.invert();
		vt.setTransform(t3d);
		
		canvas.getView().setBackClipDistance(1.05*distance+extentZ);
	}
	
	public void destroy() {
		u.removeAllLocales();
	}

	// The following allows LineTypes to be run as an application
	// as well as an applet
	//
}
