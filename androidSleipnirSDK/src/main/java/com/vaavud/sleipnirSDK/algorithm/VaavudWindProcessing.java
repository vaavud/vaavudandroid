package com.vaavud.sleipnirSDK.algorithm;

import java.lang.reflect.Array;
import java.util.Date;

import com.vaavud.sleipnirSDK.listener.SignalListener;
import com.vaavud.sleipnirSDK.listener.SpeedListener;

import android.content.Context;
import android.util.Log;

public class VaavudWindProcessing {
	
	private static int ANGLE_ITERATIONS_PR_UPDATE = 3;
	private static int REQUIRED_CALIBRATION_TICKS = 600;
	private static long UPDATE_INTERVAL = 200;//0.2F; // 5 times a second
	private static int ANGLE_CORRRECTION_COEFFICIENT = 100;
	private static int ANGLE_DIFF = 1;
	private static int SMOOTHING_TIME_CONSTANT = 4;
	private static int SMOOTHING_TIME_CONSTANT_CALIBRATION = 12;
	private static int TEETH_PR_REV = 15;
	private static int SAMPLE_FREQUENCY = 44100;

	
	private int startCounter;
	private boolean mCalibrationMode;
    
	private int[] tickLengthBuffer = new int[TEETH_PR_REV];
	private int tickLengthOneRotation;
    private float[] expTickLengthRelativePrTeeth = new float[TEETH_PR_REV];
    private float[] tickLengthRelativePrTeethCompensated = new float[TEETH_PR_REV];

	private boolean startLocated;
	private boolean exponentialFilterInitialzed;
	private int angleEstimator;
	private long previousTickTime;
	private float windSpeed = 0;
	
    private int lastTickLength;
    private float lastTickLengthCompensated;
    private int[] tickEdgeAngle = new int[TEETH_PR_REV];; // add one point in ether end

	
	private int teethIndex;
	private int tickCounterSinceStart;
	private int teethProcessIndex;

	
//	private float aSmoothingFactor = 0;
//	private boolean firstRev=true;
//    
	private int iteratorAngleCounter;
	private float[] angularVelocities = new float[TEETH_PR_REV];
	
	private SpeedListener mSpeedListener;
	private SignalListener mSignalListener;
	
	

	private Float[] compensation= {1.02127659574468F,1.02127659574468F,1.02127659574468F,1.02127659574468F,1.02127659574468F,
			1.02127659574468F,1.02127659574468F,1.02127659574468F,1.02127659574468F,1.02127659574468F,1.02127659574468F,
			1.02127659574468F,1.02127659574468F,1.02127659574468F,0.774193548387097F};

	private float[] fitcurve = {1.93055056304272F,1.92754159835895F,1.92282438491601F,1.91642240663535F,1.90836180821769F,1.89867136590046F,1.88738243346175F,1.87452883370120F,1.86014676759279F,1.84427478518094F,1.82695377850290F,1.80822697586826F,
		1.78813992874676F,1.76674047747091F,1.74407866757061F,1.72020656030400F,1.69517800715690F,1.66904843699963F,1.64187464950645F,1.61371462647876F,1.58462740924956F,1.55467305246007F,1.52391260026944F,1.49240801962532F,1.46022202221808F,
		1.42741784194637F,1.39405900931661F,1.36020913199620F,1.32593169153717F,1.29128981914961F,1.25634600129292F,1.22116175831135F,1.18579734303049F,1.15031150113437F,1.11476127584804F,1.07920182312177F,1.04368623722990F,1.00826539680125F,
		0.972987817770956F,0.937899532389511F,0.903043996582429F,0.868462039649354F,0.834191843341422F,0.800268955749256F,0.766726343538812F,0.733594507605786F,0.700901592353379F,0.668673415622578F,0.636933467547068F,0.605702923030820F,
		0.575000695079098F,0.544843511789263F,0.515245997694165F,0.486220761974393F,0.457778504155552F,0.429928136760471F,0.402676934592876F,0.376030705291920F,0.349993988555626F,0.324570272343151F,0.299762224554410F,0.275571929569200F,
		0.252001097395764F,0.229051207572156F,0.206723638918024F,0.185019804046930F,0.163941295536526F,0.143490058746097F,0.123668551764306F,0.104479853075893F,0.0859277278556616F,0.0680166393704737F,0.0507517558714560F,0.0341389996545297F,
		0.0181851042532520F,0.00289764968455452F,-0.0117149087765697F,-0.0256432058939759F,-0.0388769297985376F,-0.0514047989025574F,-0.0632145881998666F,-0.0742932098523405F,-0.0846268105605133F,-0.0942008705954160F,-0.103000306115350F,
		-0.111009589516615F,-0.118212913029394F,-0.124594370818823F,-0.130138144991804F,-0.134828678151935F,-0.138650863623146F,-0.141589940836392F,-0.143631982613669F,-0.144763880519847F,-0.144973663286989F,-0.144250763791977F,-0.142586242357516F,
		-0.139972901415060F,-0.136405374393993F,-0.131880243512971F,-0.126396155480609F,-0.119953903391895F,-0.112556485929105F,-0.104209142607944F,-0.0949193873907413F,-0.0846970613071941F,-0.0735543869171775F,-0.0615060081531991F,-0.0485690247473594F,
		-0.0347630384066071F,-0.0201102291169421F,-0.00463544268113327F,0.0116337222903312F,0.0286668458970663F,0.0464306654552773F,0.0648890733800766F,0.0840031184656525F,0.103731019912623F,0.124028212339776F,0.144847424043910F,0.166138809986866F,
		0.187850105743243F,0.209926771139125F,0.232312099871980F,0.254947302034379F,0.277771580177066F,0.300722214300621F,0.323734675998779F,0.346742778934413F,0.369678866423019F,0.392474005660318F,0.415058168415599F,0.437360424725763F,0.459309184877691F,
		0.480832480534593F,0.501858298006565F,0.522314926262134F,0.542131291505136F,0.561237285624209F,0.579564080503314F,0.597044401632675F,0.613612772959745F,0.629205754062743F,0.643762145476949F,0.657223114636108F,0.669532313041451F,0.680636049606467F,
		0.690483471159853F,0.699026703209735F,0.706220954402490F,0.712024568042803F,0.716399053179197F,0.719309119720656F,0.720722727690873F,0.720611146548223F,0.718949016219273F,0.715714393715594F,0.710888767907992F,0.704457075805069F,0.696407729977473F,
		0.686732645873142F,0.675427261400450F,0.662490538243213F,0.647924923861272F,0.631736296292326F,0.613933903685354F,0.594530298066370F,0.573541258916757F,0.550985712714184F,0.526885650621979F,0.501266033241476F,0.474154669767433F,0.445582087732282F,
		0.415581407568944F,0.384188230396930F,0.351440532507190F,0.317378563602313F,0.282044739176431F,0.245483514902245F,0.207741270922022F,0.168866239711485F,0.128908451253386F,0.0879196798867660F,0.0459533973687714F,0.00306473704801602F,
		-0.0406895385539698F,-0.0852510874443092F,-0.130560085653262F,-0.176555352635537F,-0.223174483926726F,-0.270354003694408F,-0.318029564779040F,-0.366136150084997F,-0.414608230085121F,-0.463379905156156F,-0.512385053863697F,-0.561557494697062F,
		-0.610831175836623F,-0.660140375852816F,-0.709419902462159F,-0.758605275138057F,-0.807632877266797F,-0.856440096451739F,-0.904965450293262F,-0.953148676657088F,-1.00093081662756F,-1.04825431459765F,-1.09506313749468F,-1.14130292468632F,
		-1.18692112548173F,-1.23186707424668F,-1.27609204040434F,-1.31954926810789F,-1.36219398618314F,-1.40398339226546F,-1.44487663036859F,-1.48483477692865F,-1.52382083785846F,-1.56179973308302F,-1.59873829611478F,-1.63460528731700F,-1.66937141578467F,
		-1.70300935345337F,-1.73549372023954F,-1.76680103882706F,-1.79690964091476F,-1.82579956937042F,-1.85345251794598F,-1.87985178650518F,-1.90498224798798F,-1.92883034611430F,-1.95138411094696F,-1.97263317597833F,-1.99256875417214F,-2.01118351063343F,
		-2.02847142222021F,-2.04442770651076F,-2.05904876934359F,-2.07233212953319F,-2.08427634067406F,-2.09488092934612F,-2.10414633918689F,-2.11207385653047F,-2.11866550063715F,-2.12392392855818F,-2.12785235294625F,-2.13045446941802F,-2.13173440166218F,
		-2.13169662774024F,-2.13034586385060F,-2.12768694358104F,-2.12372472526967F,-2.11846404796602F,-2.11190978398746F,-2.10406693046473F,-2.09494070314769F,-2.08453657856423F,-2.07286020228535F,-2.05991724573168F,-2.04571329439890F,-2.03025381011404F,
		-2.01354409660671F,-1.99558925551097F,-1.97639417257405F,-1.95596358148356F,-1.93430215219623F,-1.91141456498716F,-1.88730558353145F,-1.86198014106267F,-1.83544343461144F,-1.80770102675171F,-1.77875894897603F,-1.74862377338231F,-1.71730261999106F,
		-1.68480317756577F,-1.65113374186939F,-1.61630326967538F,-1.58032147957387F,-1.54319899451728F,-1.50494750075179F,-1.46557986180280F,-1.42511011063198F,-1.38355342912957F,-1.34092621637585F,-1.29724617853082F,-1.25253238496823F,-1.20680533995027F,
		-1.16008712948684F,-1.11240159666417F,-1.06377448238627F,-1.01423346987064F,-0.963808178076644F,-0.912530147553338F,-0.860432823915887F,-0.807551534849895F,-0.753923437647436F,-0.699587397418073F,-0.644583841616784F,-0.588954623685606F,
		-0.532742913754490F,-0.475993148906732F,-0.418751007243797F,-0.361063391962615F,-0.302978414524606F,-0.244545320336948F,-0.185814388853583F,-0.126836850407010F,-0.0676648561212342F,-0.00835148845376236F,0.0510492170666441F,0.110482255689112F,
		0.169891654855290F,0.229220498160153F,0.288410989747835F,0.347404532745552F,0.406141804216642F,0.464562841859978F,0.522607165069588F,0.580213909827325F,0.637321971379807F,0.693870171239719F,0.749797447763799F,0.805043053132515F,0.859546740347051F,
		0.913248923637761F,0.966090812760156F,1.01801450649125F,1.06896306768950F,1.11888059184822F,1.16771228860579F,1.21540460253994F,1.26190534933332F,1.30716384843168F,1.35113103633228F,1.39375955096661F,1.43500380728303F,1.47482010511715F,
		1.51316681498404F,1.55000456748550F,1.58529639518708F,1.61900783540342F,1.65110697646223F,1.68156447603108F,1.71035356279630F,1.73745001850842F,1.76283212588682F,1.78648061977600F,1.80837868058991F,1.82851194497160F,1.84686852509262F,
		1.86343902222370F,1.87821647622575F,1.89119628770111F,1.90237616864563F,1.91175616824101F,1.91933872768542F,1.92512873506254F,1.92913358874019F,1.93136328313521F,1.93183048501708F};
	
	private int tickLengthOneRotationLast;
	private int calibrationTickSlowdownCounter;
	private double[] tickLengthRelativePrTeethSum = new double[TEETH_PR_REV];
	private int[] tickLengthRelativePrTeethCounter = new int[TEETH_PR_REV];
	private int calibrationTickCounter;
	private int tickDetectionErrorCount;
	private float velocityProfileError;
	
	
	public VaavudWindProcessing(SpeedListener speedListener, SignalListener signalListener, boolean calibrationMode){
		mSpeedListener = speedListener;
		mSignalListener = signalListener;
		mCalibrationMode = calibrationMode;
		
		previousTickTime = 0;
		startLocated = false;
		exponentialFilterInitialzed=false;
	    float stdTickSize = 23.5F;
	    
	    tickEdgeAngle[0] = 0;
	    for (int i = 1; i < TEETH_PR_REV-1; i++) {
	        tickEdgeAngle[i] = (int) (stdTickSize*i); // shift array one to the right
	    }
	    
	    tickEdgeAngle[TEETH_PR_REV-1] = (int) (360-stdTickSize);
//	    if (calibrationMode){
//	    	resetDirectionAlgorithm();
//	    }
	}
	
	
	private void locateStart(int samples){
//		Log.d("WindProcessign","Locale Start: "+samples);
	    if (samples > 1.2 * lastTickLength && samples < 1.4 * lastTickLength) {
	        if (startCounter == 2* TEETH_PR_REV) {
	            startLocated = true;
	            lastTickLengthCompensated = samples * compensation[TEETH_PR_REV-1];
	            startCounter = 0;
	            teethIndex = 0;
	            teethProcessIndex = TEETH_PR_REV/2; //should be 7 for 15;
	        }
	        
	        if (startCounter % TEETH_PR_REV != 0) {
	            startCounter = 0;
	        }
	    }

	    if (startCounter > 2 * TEETH_PR_REV){
	    	startCounter = 0;
	    }
	    startCounter++;
	    
	}
	
	
//	@Override
	public boolean newTick(int tickLength){
	    if (!startLocated) {
	    	locateStart(tickLength);
	    	lastTickLength = tickLength;
	        return false;
	    }
	    // check if new tick value is within 20% of expected value
//		Log.d("WindProcessign","Located");
	    float tickLengthCompensated = tickLength * compensation[teethIndex];
	    
	    if (tickLengthCompensated > 0.8 * lastTickLengthCompensated && tickLengthCompensated < 1.2 * lastTickLengthCompensated) {
//	    	Log.d("WindProcessign","tickLength: "+tickLength);
	        processValidTick(tickLength);
	    } else {
//	    	Log.d("WindProcessign","ResetDirection");
	        // reset buffers
	    	resetDirectionAlgorithm();
	    	tickDetectionErrorCount++;
//	    	newTickDetectionErrorCount();
	        
	    }
	    
	    lastTickLengthCompensated = tickLengthCompensated;
	    
	    if (teethIndex == TEETH_PR_REV -1) return true;
	    else return false;

	}
	
	private void processValidTick(int tickLength){
//		Log.d("VaavudWindProcessing","ProcessValidTick");
		tickCounterSinceStart ++;
	    
	    // update tickLenghtOneRotation
	    
	    // Moving Avg subtract
	    tickLengthOneRotation -= tickLengthBuffer[teethIndex];
	    
	    // Moving avg Update buffer value
	    tickLengthBuffer[teethIndex] = tickLength;
	    
	    // Moving Avg update SUM
	    tickLengthOneRotation += tickLengthBuffer[teethIndex];
	    
	    
	    if (mCalibrationMode) {
	        if (tickLengthOneRotation > tickLengthOneRotationLast) {
	            calibrationTickSlowdownCounter++;
	        } else {
	            calibrationTickSlowdownCounter = 0;
	        }
//	        Log.d("SleipnirSDK","tickLengthOneRotation: "+tickLengthOneRotation + " calibrationTickSlowdownCounter: "+calibrationTickSlowdownCounter);
//	        if (calibrationTickSlowdownCounter > 200 && tickLengthOneRotation > 750 && initializeExponentialFilter) {
	        if (tickLengthOneRotation < 5000 && calibrationTickSlowdownCounter > 100 && tickLengthOneRotation > 2000) {
	            // Filter Calibration
	            if (!exponentialFilterInitialzed) {
		        	initializeExponentialFilter();
		        	
		            // Mean Calibration
//		        	Log.d("VaavudWindProcessing","Mean Calibration");
		            for (int i = 0; i < TEETH_PR_REV; i++) {
//		            	Log.d("SleipnirSDK","Mean Calibration: "+i);
		                tickLengthRelativePrTeethSum[i] = 0;
		                tickLengthRelativePrTeethCounter[i] = 0;
		            }
	            }
	        
	        updateExponentialFilter();
	        
	     // Average Calibration
            double tickLengthRelative = tickLengthBuffer[teethProcessIndex] * TEETH_PR_REV / (double) tickLengthOneRotation;
            tickLengthRelativePrTeethSum[teethProcessIndex] += tickLengthRelative;
            tickLengthRelativePrTeethCounter[teethProcessIndex] += 1;
            
            calibrationTickCounter++;
//            Log.d("SleipnirSDK","tickLengthOneRotation: "+tickLengthOneRotation + " calibrationTickSlowdownCounter: "+calibrationTickSlowdownCounter);
            if (calibrationTickCounter == REQUIRED_CALIBRATION_TICKS) {
	                endCalibration();
	                mSpeedListener.calibrationPercentageComplete(calibrationTickCounter / (float) REQUIRED_CALIBRATION_TICKS);
            }
//	            Log.d("VaavudWindProcessing","Previous Tick Time:" + previousTickTime);
	        }

	    }
	    else{
		    if (tickCounterSinceStart == TEETH_PR_REV && exponentialFilterInitialzed){
		    	initializeExponentialFilter();
		    }
		    if (tickCounterSinceStart > TEETH_PR_REV) {
		        updateExponentialFilter();
		    }
	    }
	    
	 // update results
        Date time = new Date();
        if ( time.getTime() > previousTickTime + UPDATE_INTERVAL ) {
            updateUI();
            previousTickTime = time.getTime();
        }
        
	    teethIndex++;
	    if (teethIndex == TEETH_PR_REV) {
	        teethIndex = 0;
	    }
	    teethProcessIndex++;
	    if (teethProcessIndex == TEETH_PR_REV) {
	        teethProcessIndex = 0;
	    }
//	    Log.d("SleipnirSDK","TeethIndex: "+teethIndex);
	    tickLengthOneRotationLast = tickLengthOneRotation;

	}
	
	private void initializeExponentialFilter(){
//		Log.d("SleipnirSDK","InitializeExponentialFilter: "+tickLengthOneRotation);
		for (int i=0; i< TEETH_PR_REV; i++) {
            expTickLengthRelativePrTeeth[i] = tickLengthBuffer[i] * TEETH_PR_REV / (float) tickLengthOneRotation;
        }
		exponentialFilterInitialzed = true;
	}
	
	private void updateExponentialFilter(){
		// calculate relative tick length
//		Log.d("SleipnirSDK","UpdateExponentialFilter: "+tickLengthOneRotation);
        float tickLengthRelative = tickLengthBuffer[teethProcessIndex] * TEETH_PR_REV / (float) tickLengthOneRotation;
        
        float a_smoothingFactor;
        if (mCalibrationMode){
        	a_smoothingFactor = 3 * tickLengthBuffer[teethProcessIndex] * TEETH_PR_REV / (float) ( SMOOTHING_TIME_CONSTANT_CALIBRATION * SAMPLE_FREQUENCY);
        }else{
        	a_smoothingFactor = 3 * tickLengthBuffer[teethProcessIndex] * TEETH_PR_REV / (float) ( SMOOTHING_TIME_CONSTANT * SAMPLE_FREQUENCY);
        }
        
        expTickLengthRelativePrTeeth[teethProcessIndex] = a_smoothingFactor * tickLengthRelative + (1 - a_smoothingFactor) * expTickLengthRelativePrTeeth[teethProcessIndex];
	}
	
	private void resetDirectionAlgorithm(){
//		Log.d("SleipnirSDK","Reset Direction Algorithm");
		for (int i = 0; i < TEETH_PR_REV; i++) {
            tickLengthBuffer[i] = 0;
        }
        tickLengthOneRotation = 0;
        tickLengthOneRotationLast = 0;
        tickCounterSinceStart = 0;
        startLocated = false;	
	}
	
	private void endCalibration() {

	    if (calibrationTickCounter >= REQUIRED_CALIBRATION_TICKS) {
	    	calculateMeanFilter();
	        // Exp Filter
//	        double compensationSum = 0;
//	        
//	        for (int i = 0; i < TEETH_PR_REV; i++) {
//	            compensationSum += expTickLengthRelativePrTeeth[i];
//	        }
//	        
//	        double adjustmentRatio = TEETH_PR_REV / compensationSum;
//	        
//	        
//	        for (int i = 0; i < TEETH_PR_REV; i++) {
//	            compensation[i] = (float) (1/(expTickLengthRelativePrTeeth[i]*adjustmentRatio));
//	        }
	    	printArray(compensation, "Compensation");
		    mCalibrationMode = false;
		    mSpeedListener.calibrationCoefficients(compensation);
	    }
	    resetDirectionAlgorithm();
	    exponentialFilterInitialzed = false;
	    
	    
	}
	
	private void calculateMeanFilter() {
	    // Mean Filter
	    float compensationSum = 0;
	    float[] tickLengthRelativePrTeethAvg = new float[TEETH_PR_REV];
	    
	    for (int i = 0; i < TEETH_PR_REV; i++) {
	        tickLengthRelativePrTeethAvg[i] = (float) (tickLengthRelativePrTeethSum[i]/ tickLengthRelativePrTeethCounter[i]);
	        compensationSum += tickLengthRelativePrTeethAvg[i];
	    }
	    double adjustmentRatio = TEETH_PR_REV / compensationSum;
	    for (int i = 0; i < TEETH_PR_REV; i++) {
	        compensation[i] = (float) (1/(tickLengthRelativePrTeethAvg[i]*adjustmentRatio));
	    }
	}
	
	private void updateUI() {
//		Log.d("SleipnirSDK","UPDATE UI");
	    // Calculate relative velocities
		
		float[] tickLengthRelativePrTeethCompensated = new float[TEETH_PR_REV];

		for (int i = 0; i < TEETH_PR_REV; i++) {
	        tickLengthRelativePrTeethCompensated[i] = (expTickLengthRelativePrTeeth[i] * compensation[i] -1) * (-100); // - compensationS12[i]; // - compentationT1Ispo[i];
	        angularVelocities[i] = tickLengthRelativePrTeethCompensated[i];
	    }
		
	    windSpeed = SAMPLE_FREQUENCY / ((float)tickLengthOneRotation);
//	    Log.d("WindProcessign","WindSpeed: "+windSpeed);
//	    Log.d("WindProcessign","Speed: "+windSpeed + " SamplesPrLastRotation: "+ samplesPrLastRotation);
	    for (int i = 0; i < ANGLE_ITERATIONS_PR_UPDATE; i++) {
	    	iterateAngle(tickLengthRelativePrTeethCompensated);
	    }
	    if (mCalibrationMode){
//	    	Log.d("SleipnirSDK","Calibration percentage: "+ calibrationTickCounter / (float) REQUIRED_CALIBRATION_TICKS);
	    	mSpeedListener.calibrationPercentageComplete(calibrationTickCounter / (float) REQUIRED_CALIBRATION_TICKS);
	    }
	    else {
	    	mSpeedListener.speedChanged(windSpeed, angleEstimator, new Date().getTime(),velocityProfileError);
	    }
       
	    if(mSignalListener!=null){
	    	float[] angleCurve = new float[15];
	    	for (int i=0;i<angleCurve.length;i++) angleCurve[i] = fitcurve[((tickEdgeAngle[i]-angleEstimator)+360)%360];
    		mSignalListener.signalChanged(angularVelocities, angleCurve);
    	}
		
	}
	
	public String getStringAvgAngularVelocities(){
		String angularString = "";
		for (int i = 0; i < TEETH_PR_REV; i++) {
			angularString += i + ":" + tickLengthRelativePrTeethCompensated[i] + "\n";
		}
		return angularString;
	}

	private void iterateAngle(float[] mvgRelativeSpeedPercent) {
	    
	       // SMALL NOTICE (ANGLES IN USE ARE EDGE ANGLESF, MIGHT BE BETTER TO CALCULATE EXCATE ANGLES!)
	    
	    if (iteratorAngleCounter == 5) {
	        checkOppositeAngle(mvgRelativeSpeedPercent);
	        iteratorAngleCounter = 0;
	    }
	    
	    int angleLow = (int) Math.floor(angleEstimator - 0.5);
	    int angleHigh = angleLow + 1;
	    
	    if (angleLow < 0)
	        angleLow += 360;
	    
	    if (angleHigh > 360)
	        angleHigh -= 360;
	    
	    float angleLowSum = 0.0F;
	    float angleHighSum = 0.0F;
	    
	    for (int i = 0; i < TEETH_PR_REV; i++) {
	        
	        int signalExpectedIndexLow = tickEdgeAngle[i] - angleLow;
	        if (signalExpectedIndexLow < 0)
	            signalExpectedIndexLow += 360;
	        
	        int signalExpectedIndexHigh = tickEdgeAngle[i] - angleHigh;
	        if (signalExpectedIndexHigh < 0)
	            signalExpectedIndexHigh += 360;
	        
	        angleLowSum += Math.pow(fitcurve[signalExpectedIndexLow]-mvgRelativeSpeedPercent[i], 2.0);
	        angleHighSum += Math.pow(fitcurve[signalExpectedIndexHigh]-mvgRelativeSpeedPercent[i], 2.0);
	    }

	    float angleHLDiff = (angleLowSum - angleHighSum)/ (float) TEETH_PR_REV;
	    angleEstimator += angleHLDiff * ANGLE_CORRRECTION_COEFFICIENT;
	    
	    if (angleEstimator < 0) {
	        angleEstimator += 360;
	    }
	    
	    if (angleEstimator > 360)
	        angleEstimator -= 360;
	    
	    iteratorAngleCounter++;
	    velocityProfileError = angleLowSum;
	    
	}
	
	private void checkOppositeAngle(float[] mvgRelativeSpeedPercent) {
	    
	    // SMALL NOTICE (ANGLES IN USE ARE EDGE ANGLESF, MIGHT BE BETTER TO CALCULATE EXCATE ANGLES!)
	    
	    int angleLow = (angleEstimator);
	    int angleHigh = angleEstimator + 180;
	    
	    if (angleLow < 0)
	        angleLow += 360;
	    
	    if (angleHigh > 360)
	        angleHigh -= 360;
	    
	    float angleLowSum = 0.0F;
	    float angleHighSum = 0.0F;
	    
	    for (int i = 0; i < TEETH_PR_REV; i++) {
	        
	        int signalExpectedIndexLow = tickEdgeAngle[i] - angleLow;
	        if (signalExpectedIndexLow < 0)
	            signalExpectedIndexLow += 360;
	        
	        int signalExpectedIndexHigh = tickEdgeAngle[i] - angleHigh;
	        if (signalExpectedIndexHigh < 0)
	            signalExpectedIndexHigh += 360;
	        
	        angleLowSum += Math.pow(fitcurve[signalExpectedIndexLow]-mvgRelativeSpeedPercent[i], 2.0);
	        angleHighSum += Math.pow(fitcurve[signalExpectedIndexHigh]-mvgRelativeSpeedPercent[i], 2.0);
	    }

	    if (angleLowSum > angleHighSum){
	    	angleEstimator+=180;
	    }
	    if (angleEstimator > 360){
	    	angleEstimator-=360;
	    }
	}
	
	public void close(){
		tickLengthBuffer = null;
		expTickLengthRelativePrTeeth = null;
		tickLengthRelativePrTeethCompensated = null;
		tickLengthRelativePrTeethCounter = null;
		tickLengthRelativePrTeethSum = null;
		tickEdgeAngle = null;
		compensation = null;
		fitcurve = null;
		mSignalListener = null;
		mSpeedListener = null;
	}


	public void setCoefficients(Float[] coefficients) {
		if (!mCalibrationMode && coefficients!=null){
			System.arraycopy(coefficients, 0, compensation, 0, coefficients.length);
		}
	}
	
	private void printArray(Object[] array, String name){
		String vector = name+": ["+array[0];
        for (int i=1;i<expTickLengthRelativePrTeeth.length; i++) vector+=","+array[i];
        Log.d("SleipnirSDK",vector+"]");
	}
}
