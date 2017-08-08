package br.unicamp.fnjv.wasis.main.tests;

import com.leandrotacioli.libs.LTDataTypes;

import br.unicamp.fnjv.wasis.database.DatabaseMySQLConnection;
import br.unicamp.fnjv.wasis.features.PLP;
import br.unicamp.fnjv.wasis.multimidia.wav.AudioWav;

public class ExtractPLP implements Runnable {
	private static String SERVER = "localhost";
	private static String DBNAME = "wasis_mestrado";
	private static String DBUSER = "root";
	private static String DBPASS = "123456";
	
	private AudioWav objAudioWav;
	
	private int intIdAudioSegment;
	private int intInitialTime;
	private int intFinalTime;
	
	/**
	 * 
	 * @param strAudioFilePath
	 * @param intInitialChunk
	 * @param intFinalChunk
	 * 
	 * @throws CloneNotSupportedException
	 */
	protected ExtractPLP(AudioWav objAudioWav, int intInitialTime, int intFinalTime) throws CloneNotSupportedException {
		this.intIdAudioSegment = 0;
		this.objAudioWav = (AudioWav) objAudioWav.clone();
		
		this.intInitialTime = intInitialTime;
		this.intFinalTime = intFinalTime;
	}
	
	/**
	 * 
	 * @param strAudioFilePath
	 * @param intInitialChunk
	 * @param intFinalChunk
	 * 
	 * @throws CloneNotSupportedException
	 */
	protected ExtractPLP(int intIdAudioSegment, AudioWav objAudioWav, int intInitialTime, int intFinalTime) throws CloneNotSupportedException {
		this.intIdAudioSegment = intIdAudioSegment;
		this.objAudioWav = (AudioWav) objAudioWav.clone();
		
		this.intInitialTime = intInitialTime;
		this.intFinalTime = intFinalTime;
	}
	
	@Override
	public void run() {
		int intInitialChunkToProcess = objAudioWav.getSampleFromTime(intInitialTime);
		int intFinalChunkToProcess = objAudioWav.getSampleFromTime(intFinalTime);
		
		double[] arrayAmplitudes = objAudioWav.getAmplitudesChunk(1, intInitialChunkToProcess, intFinalChunkToProcess);
		
		PLP objPLP = new PLP();
		objPLP.process(arrayAmplitudes, objAudioWav.getWavHeader().getSampleRate());
		
		double[][] plp = objPLP.getPLP();
		double[] mean = objPLP.getMean();
		double[] standardDeviation = objPLP.getStandardDeviation();
		
		/*
		for (int indexFrame = 0; indexFrame < plp.length; indexFrame++) {
			System.out.print("PLP: " + indexFrame + "	");
        	for (int indexCoefficient = 0; indexCoefficient < plp[indexFrame].length; indexCoefficient++) {
        		System.out.print(plp[indexFrame][indexCoefficient] + "	");
        	}
        	System.out.println("");
		}
		
		System.out.println("");
		
		System.out.print("Mean: " + "	");
		for (int i = 0; i < mean.length; i++) {
    		System.out.print(mean[i] + "	");
    	}
		System.out.println("");
		
		System.out.print("SD: " + "	");
		for (int i = 0; i < standardDeviation.length; i++) {
    		System.out.print(standardDeviation[i] + "	");
    	}
		System.out.println("");
		System.out.println("-----------------------------------");
		*/
		
		// Salva as features no banco de dados
		if (intIdAudioSegment != 0) {
			DatabaseMySQLConnection objDatabaseConnection = new DatabaseMySQLConnection();
			objDatabaseConnection.setDatabaseConnection(SERVER, DBNAME, DBUSER, DBPASS);
			objDatabaseConnection.openConnection();
			
			try {
				// Mean & Standard Deviation
				{
					StringBuffer strFeatureMeanSDVector = new StringBuffer();
					String strMeanSDVector;
					
					for (int indexVector = 0; indexVector < mean.length; indexVector++) {
						strFeatureMeanSDVector.append(mean[indexVector] + ";");
					}
					
					for (int indexVector = 0; indexVector < standardDeviation.length; indexVector++) {
						strFeatureMeanSDVector.append(standardDeviation[indexVector] + ";");
					}
					
					strMeanSDVector = strFeatureMeanSDVector.toString();
					
					// Remove o último caracter que é um ';'
					if (strMeanSDVector.substring(strMeanSDVector.length() - 1).equals(";")) {
						strMeanSDVector = strMeanSDVector.substring(0, strMeanSDVector.length() - 1);
					}
					
					objDatabaseConnection.initiliazeStatement();
					objDatabaseConnection.sqlCommand("INSERT INTO audio_files_segments_plp (fk_audio_file_segment, ind_normalized, plp_order, plp_vector) ");
					objDatabaseConnection.sqlCommandAppend("VALUES (?, ?, ?, ?) ");
					objDatabaseConnection.addParameter("fk_audio_file_segment", LTDataTypes.INTEGER, intIdAudioSegment);
					objDatabaseConnection.addParameter("ind_normalized", LTDataTypes.BOOLEAN, Boolean.TRUE);
					objDatabaseConnection.addParameter("plp_order", LTDataTypes.INTEGER, 0);
					objDatabaseConnection.addParameter("plp_vector", LTDataTypes.STRING, strMeanSDVector);
					objDatabaseConnection.executeQuery();
				}
				
				// Features
				{
					StringBuffer strFeatureVector;
					String strVector;

					for (int indexVector = 0; indexVector < plp.length; indexVector++) {
						strFeatureVector = new StringBuffer();
						
						for (int indexValue = 0; indexValue < plp[indexVector].length; indexValue++) {
							strFeatureVector.append(plp[indexVector][indexValue] + ";");
						}
						
						strVector = strFeatureVector.toString();
						
						// Remove o último caracter que é um ';'
						if (strVector.substring(strVector.length() - 1).equals(";")) {
							strVector = strVector.substring(0, strVector.length() - 1);
						}
						
						objDatabaseConnection.initiliazeStatement();
						objDatabaseConnection.sqlCommand("INSERT INTO audio_files_segments_plp (fk_audio_file_segment, ind_normalized, plp_order, plp_vector) ");
						objDatabaseConnection.sqlCommandAppend("VALUES (?, ?, ?, ?) ");
						objDatabaseConnection.addParameter("fk_audio_file_segment", LTDataTypes.INTEGER, intIdAudioSegment);
						objDatabaseConnection.addParameter("ind_normalized", LTDataTypes.BOOLEAN, Boolean.FALSE);
						objDatabaseConnection.addParameter("plp_order", LTDataTypes.INTEGER, indexVector + 1);
						objDatabaseConnection.addParameter("plp_vector", LTDataTypes.STRING, strVector);
						objDatabaseConnection.executeQuery();
					}
				}
				
				objDatabaseConnection.commitTransaction();
				
			} catch (Exception e) {
				e.printStackTrace();
				
			} finally {
				objDatabaseConnection.rollBackTransaction();
				objDatabaseConnection.closeConnection();
			}
		}
	}
}