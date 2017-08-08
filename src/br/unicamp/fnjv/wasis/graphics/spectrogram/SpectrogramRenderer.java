package br.unicamp.fnjv.wasis.graphics.spectrogram;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import br.unicamp.fnjv.wasis.dsp.FFTParameters;
import br.unicamp.fnjv.wasis.features.PowerSpectrumValues;

/**
 * Classe responsável pela renderização e 
 * criação das imagens do espectrograma.
 * 
 * @author Leandro Tacioli
 * @version 2.2 - 02/Set/2015
 */
public class SpectrogramRenderer {
	private Spectrogram objSpectrogram;

	/**
	 * Tipos de renderização permitida no espectrograma.<br>
	 * <br>
	 * 0 - RENDER_DEFAULT = Renderização padrão do espectrograma. <br>
	 * 1 - RENDER_TEMPORARY = Renderização para imagens temporárias do espectrograma. <br>
	 * 2 - RENDER_COMPARISON = Renderização para extração de dados para comparação e/ou gravação no banco de dados.
	 */
	private int intRenderType;
	
	private int intInitialFrequencyFrame;      // Frame de frequência inicial
	private int intFinalFrequencyFrame;        // Frame de frequência final
	
	private long lgnMaximumAmplitude;          // Amplitude máxima encontrada no arquivo de áudio
	
	private double dblMinimumIntensity;        // Intensidade mínima (dB) encontrada no arquivo de áudio
	private double dblMaximumIntensity;        // Intensidade máxima (dB) encontrada no arquivo de áudio
	
	private double dblMaximumFrequency;        // Frequência máxima encontrada no arquivo de áudio
	
	/**
	 * Lista com as intensidades do arquivo de áudio
	 */
	private List<SpectrogramRendererIntensities> lstIntensities;
	
	/**
	 * Lista com os valores de frequência e a intensidade máxima (dB) para cada faixa de frequência.
	 */
	private List<PowerSpectrumValues> lstAudioComparisonValues;
	
	/**
	 * Retorna o objeto do <i>Spectrogram</i>.
	 * 
	 * @return objSpectrogram
	 */
	protected Spectrogram getSpectrogram() {
		return objSpectrogram;
	}
	
	/**
	 * Retorna o tipo de renderização do espectrograma solicitada. <br>
	 * <br>
	 * 0 - RENDER_DEFAULT = Renderização padrão do espectrograma. <br>
	 * 1 - RENDER_TEMPORARY = Renderização para imagens temporárias do espectrograma. <br>
	 * 2 - RENDER_COMPARISON = Renderização para extração de dados para comparação e/ou gravação no banco de dados.
	 * 
	 * @return intRenderType
	 */
	protected int getRenderType() {
		return intRenderType;
	}
	
	/**
	 * Retorna a amplitude máxima encontrada no arquivo de áudio.
	 * 
	 * @return lgnMaximumAmplitude
	 */
	protected long getMaximumAmplitude() {
		return lgnMaximumAmplitude;
	}
	
	/**
	 * Retorna o valor da intensidade mínima encontrada no arquivo de áudio.
	 * 
	 * @return dblMinimumIntensity
	 */
	protected double getMinimumIntensity() {
		return dblMinimumIntensity;
	}

	/**
	 * Altera o valor da intensidade mínima encontrada no arquivo de áudio.
	 * 
	 * @param dblMinimumIntensity
	 */
	protected void setMinimumIntensity(double dblMinimumIntensity) {
		this.dblMinimumIntensity = dblMinimumIntensity;
	}
	
	/**
	 * Retorna o valor da intensidade máxima encontrada no arquivo de áudio.
	 * 
	 * @return dblMaximumIntensity
	 */
	protected double getMaximumIntensity() {
		return dblMaximumIntensity;
	}

	/**
	 * Altera o valor da intensidade máxima encontrada no arquivo de áudio.
	 * 
	 * @param dblMaximumIntensity
	 */
	protected void setMaximumIntensity(double dblMaximumIntensity) {
		this.dblMaximumIntensity = dblMaximumIntensity;
	}
	
	/**
	 * Retorna o valor da frequência máxima encontrada no arquivo de áudio.
	 * 
	 * @return dblMaximumFrequency
	 */
	protected double getMaximumFrequency() {
		return dblMaximumFrequency;
	}
	
	/**
	 * Retorna a lista com os valores de frequência 
	 * e a intensidade máxima (dB) para cada faixa de frequência.
	 * 
	 * @return lstAudioComparisonValues
	 */
	protected List<PowerSpectrumValues> getAudioComparisonValues() {
		return lstAudioComparisonValues;
	}
	
	/**
	 * Classe responsável pela renderização e 
	 * criação das imagens do espectrograma.
	 * 
	 * @param objSpectrogram - Objeto do Espectrograma
	 * @param intRenderType  - Tipo de renderização do espectrograma
	 */
	protected SpectrogramRenderer(Spectrogram objSpectrogram, int intRenderType) {
		this.objSpectrogram = objSpectrogram;
		this.intRenderType = intRenderType;
		
		// Inicializa lista com os valores de frequência e a intensidade máxima (dB) para cada faixa de frequência
		if (this.intRenderType == getSpectrogram().RENDER_COMPARISON) {
			int intFrequencySamples = FFTParameters.FFT_SAMPLE_SIZE_COMPARISON / 2;
	    	
	    	dblMaximumFrequency = (double) objSpectrogram.getMaximumFrequency();
	    	
	    	lstAudioComparisonValues = new ArrayList<PowerSpectrumValues>();
	    	
	    	for (int indexFrequency = intFrequencySamples - 1; indexFrequency >= 0; indexFrequency--) {
	    		double dblFrequency = objSpectrogram.getMaximumFrequency() - (dblMaximumFrequency / (double) intFrequencySamples * indexFrequency);
	    		int intFrequency = (int) dblFrequency;
	    		
	    		lstAudioComparisonValues.add(new PowerSpectrumValues(intFrequency, -100000));
	        }
		}

		lgnMaximumAmplitude = Math.abs(1 << (objSpectrogram.getAudioWav().getWavHeader().getBitsPerSample() - 1));
    	
    	dblMaximumIntensity = Double.MIN_VALUE;
	}
	
	/**
	 * Executa a renderização do espectrograma.
	 * 
	 * @param intInitialTime - Tempo inicial
	 * @param intFinalTime   - Tempo final
	 */
	protected void executeRenderer(int intInitialTime, int intFinalTime) {
		try {
			lstIntensities = new ArrayList<SpectrogramRendererIntensities>();
			
			// Caso o número final de pixels de comprimento for maior que o painel onde o espectrograma 
	    	// será desenhado, o sistema irá automaticamente ajustar o 'Frame Size' e o 'Frame Shift' até que 
	    	// o número final de pixels corresponda ao tamanho do painel;
			double dblMillisecondsPerPixel = FFTParameters.getInstance().getMillisecondsPerPixel(FFTParameters.getInstance().getFFTSampleSize());
			
			// Caso a tipo de renderização seja para comparação, o valor de milisegundos por pixel
			// será baseado na constante 'FFTParameters.FFT_SAMPLE_SIZE_COMPARISON'
			if (intRenderType == getSpectrogram().RENDER_COMPARISON) {
				dblMillisecondsPerPixel = FFTParameters.getInstance().getMillisecondsPerPixel(FFTParameters.FFT_SAMPLE_SIZE_COMPARISON);
			}
			
			int intFrameSize = getFrameSize(dblMillisecondsPerPixel);  // Tamanho final do frame (em samples)
	    	int intFrameShift = getFrameShift(intFrameSize);
	    	int intOverlapBackSamples = intFrameSize - intFrameShift;  // Número de amostras que serão retornadas quando houver OVERLAP
	    	
	    	// Todos os valores de chunk correspondem ao valor da amostra 'sample' baseado no tempo do áudio
	    	int intInitialChunkToProcess = objSpectrogram.getAudioWav().getSampleFromTime(intInitialTime);  // Pedaço inicial do áudio a ser processado
	    	int intFinalChunkToProcess = objSpectrogram.getAudioWav().getSampleFromTime(intFinalTime);      // Pedaço final do áudio a ser processado
	    	
	    	int intInitialChunkProcessing = intInitialChunkToProcess;                      // Pedaço inicial do áudio em processamento
			int intFinalChunkProcessing = intInitialChunkProcessing + intFrameSize - 1;    // Pedaço final do áudio em processamento
	    	
			// Caso a imagem tenha um tamanho grande e a renderização seja a padrão
	    	if (!objSpectrogram.getIsShortTemporaryImage() && intRenderType == objSpectrogram.RENDER_DEFAULT) {
		    	boolean blnAdjustSpectrogram = true;
		    	
		    	while (blnAdjustSpectrogram) {
		    		int intNumTotalWidthPixels = 0;
		    		
		    		intInitialChunkProcessing = intInitialChunkToProcess;
		    		intFinalChunkProcessing = intInitialChunkProcessing + intFrameSize - 1;
		    		
		    		// Realiza a leitura do arquivo até chegar ao final do pedaço de áudio a ser processado
		    		for (int indexChunkProcessing = intInitialChunkProcessing; indexChunkProcessing <= intFinalChunkToProcess;) {
		    			
		    			// Sem OVERLAP
			    		if (FFTParameters.getInstance().getFFTOverlapFactor() == 0) {
			    			intInitialChunkProcessing += intFrameSize;
			    			intFinalChunkProcessing += intFrameSize;
			    			
			    		// Com OVERLAP
			    		} else {
			    			intInitialChunkProcessing = intFinalChunkProcessing - intOverlapBackSamples + 1;
			    			intFinalChunkProcessing = intInitialChunkProcessing + intFrameSize - 1;
			    		}
	
			    		intNumTotalWidthPixels++;
			    		indexChunkProcessing = intInitialChunkProcessing;
			        }
		    		
			        // Verifica se o número de pixels a ser gerado é maior que o tamanho do painel onde será desenhado.
			        // Reajusta o 'Frame Size' e o 'Frame Shift' até encontrar um valor que satisfaça a condição acima.
		    		if (intNumTotalWidthPixels > objSpectrogram.getPanelWidth()) {
		    			dblMillisecondsPerPixel = dblMillisecondsPerPixel + 0.05;   // Adiciona mais 50 milisegundos
				        intFrameSize = getFrameSize(dblMillisecondsPerPixel);
				        intFrameShift = getFrameShift(intFrameSize);
				        intOverlapBackSamples = intFrameSize - intFrameShift;
			        } else {
			        	blnAdjustSpectrogram = false;
			        	break;
			        }
		    	}
	    	}
	    	
	    	// ***********************************************************************************************
	    	// Começa a leitura do arquivo de áudio
	     	intInitialChunkProcessing = intInitialChunkToProcess;
	     	intFinalChunkProcessing = intInitialChunkProcessing + intFrameSize - 1;
	     	
			objSpectrogram.getAudioWav().extractWavDataChunk(intInitialChunkProcessing);
			
	        // Retorna a quantidade total de processadores (núcleos) que poderão ser utilizados no carregamento do espectrograma
	     	int intNumberProcessors = Runtime.getRuntime().availableProcessors();
	     	
	     	if (intRenderType != objSpectrogram.RENDER_DEFAULT && intNumberProcessors > 1) {
	     		//intNumberProcessors = intNumberProcessors - 1;
	     	}
	     	
	     	// É criada uma pool com uma thread para cada processador disponível para o carregamento do espectrograma
			ExecutorService executorService = Executors.newFixedThreadPool(intNumberProcessors);
			
			// Realiza a leitura do arquivo até chegar ao final do pedaço de áudio a ser processado
			for (int indexChunkProcessing = intInitialChunkProcessing; indexChunkProcessing <= intFinalChunkToProcess;) {
				if (objSpectrogram.getAudioWav().getChunkDataPosition(intInitialChunkProcessing) < objSpectrogram.getAudioWav().getWavDataInitialPosition() || objSpectrogram.getAudioWav().getChunkDataPosition(intFinalChunkProcessing) > objSpectrogram.getAudioWav().getWavDataFinalPosition()) {
	    			if (objSpectrogram.getAudioWav().getChunkDataPosition(intFinalChunkProcessing) < objSpectrogram.getAudioWav().getWavDataSize()) {
						objSpectrogram.getAudioWav().extractWavDataChunk(intInitialChunkProcessing);
	    			}
				}
				
				SpectrogramRendererThread objSpectrogramRendererThread = new SpectrogramRendererThread(this, intInitialChunkProcessing, intFinalChunkProcessing);
				executorService.execute(objSpectrogramRendererThread);
				
				// ***********************************************************************************************
				// Pedaços iniciais e finais são atualizados a cada loop
				if (intRenderType != objSpectrogram.RENDER_COMPARISON) {
					
					// Sem OVERLAP
		    		if (FFTParameters.getInstance().getFFTOverlapFactor() == 0) {
		    			intInitialChunkProcessing += intFrameSize;
		    			intFinalChunkProcessing += intFrameSize;
		    		
		    		// Com OVERLAP
		    		} else {
		    			intInitialChunkProcessing = intFinalChunkProcessing - intOverlapBackSamples + 1;
		    			intFinalChunkProcessing = intInitialChunkProcessing + intFrameSize - 1;
		    		}
		    		
				} else if (intRenderType == objSpectrogram.RENDER_COMPARISON) {
		    		intInitialChunkProcessing = intFinalChunkProcessing - intOverlapBackSamples + 1;
		    		intFinalChunkProcessing = intInitialChunkProcessing + intFrameSize - 1;
				}

	    		indexChunkProcessing = intInitialChunkProcessing;
			}
			
			executorService.shutdown();
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);  // Aguarda finalizar todas as threads
			
	    	Collections.sort(lstIntensities); // Ordena a lista de intensidades
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
     * Retorna o tamanho final de cada frame (em samples).
     * 
     * @param dblMillisecondsPerPixel - Valor em milisegundos que cada pixel gerado no espectrograma terá.
     * 
     * @return intFrameSize
     */
    private int getFrameSize(double dblMillisecondsPerPixel) {
    	int intFrameSize = (int) (objSpectrogram.getAudioWav().getNumSamplesPerChannel() / (objSpectrogram.getAudioWav().getTotalTime() / dblMillisecondsPerPixel));

    	return intFrameSize;
    }
    
    /**
     * Retorna o espaçamento entre um frame e outro. 
     * 
     * @param intFrameSize - Tamanho final do frame
     * 
     * @return intFrameShift
     */
    private int getFrameShift(int intFrameSize) {
    	// Números são transformados em 'double' para garantir uma maior precisão
    	double dblFrameSize = intFrameSize;
    	double dblFFTOverlapFactor = FFTParameters.getInstance().getFFTOverlapFactor();
    	double dblFrameShift = dblFrameSize - (dblFrameSize * dblFFTOverlapFactor / 100.0d);
    	
    	int intFrameShift = (int) Math.floor(dblFrameShift);   // Transforma novamente em inteiro arredondando para baixo
    	
    	return intFrameShift;
    }
  
	/**
	 * Adiciona as intensidades à lista <i>lstIntensities</i>
	 * quando uma thread da pool for finalizada.
	 * 
	 * @param intInitialChunk
	 * @param intensities
	 */
    protected synchronized void endThread(int intInitialChunk, double[] intensities) {
		lstIntensities.add(new SpectrogramRendererIntensities(intInitialChunk, intensities));
    }
    
    /**
     * Retorna a imagem do espectrograma.
     * 
     * @param blnAllFrequencyFrames - Considera todos os frames de frequência
     * 
     * @return spectrogramImage
     */
    protected BufferedImage getSpectrogramImage(boolean blnAllFrequencyFrames) {
        int intTimeFrames = lstIntensities.size();                                 // Eixo X - Tempo
        int intFrequencyFrames = getNumberFrequencyFrames(blnAllFrequencyFrames);  // Eixo Y - Frequência
        
        BufferedImage spectrogramImage = new BufferedImage(intTimeFrames, intFrequencyFrames, BufferedImage.TYPE_INT_RGB);
        
        double[] intensities;
        
        // ******************************************************************************************************
    	for (int indexTimeFrame = 0; indexTimeFrame < intTimeFrames; indexTimeFrame++) {
    		intensities = lstIntensities.get(indexTimeFrame).getIntensities();

            for (int indexFrequencyFrame = intInitialFrequencyFrame; indexFrequencyFrame < intFinalFrequencyFrame; indexFrequencyFrame++) {
            	//double dblDecibel = intensities[indexFrequencyFrame] - 96.00d; // 96dB range para 16 bits
            	double dblDecibel = intensities[indexFrequencyFrame]; // Adiciona o range na FFT
            	
            	int[] intRGB = new int[]{0, 0, 0};
            	
            	int[] intInitialGradient = new int[] {255, 255, 255};
        		int[] intFinalGradient = new int[] {0, 0, 0};
            	
        		if (dblDecibel >= 0) {
        			intRGB = intFinalGradient;
        		} else if (dblDecibel < -100) {
        			intRGB = intInitialGradient;
        		} else {
        			intRGB = getSpectrogramColor(dblDecibel, -100, 0, intInitialGradient, intFinalGradient);
        		}
            	
            	int intRGBValue = ((intRGB[0] << 16) & 0xff0000) | ((intRGB[1] << 8) & 0xff00) | (intRGB[2] & 0xff);
            	spectrogramImage.setRGB(indexTimeFrame, intFinalFrequencyFrame - indexFrequencyFrame - 1, intRGBValue);
            }
    	}
    	
    	return spectrogramImage;
    }
    
    /**
     * Retorna um vetor com as cores RGB.
     * 
     * @param dblDecibel         - Valor a ser atribuída uma cor
     * @param dblMinimumValue    - Valor mínimo de decibéis referente ao início do gradiente
     * @param dblMaximumValue    - Valor máximo de decibéis referente ao final do gradiente
     * @param intInitialGradient - Início do gradiente
     * @param intFinalGradient   - Fim do gradiente
     * 
     * @return intRGB[0] = Red <br>
     *         intRGB[1] = Green <br>
     *         intRGB[2] = Blue
     */
    private int[] getSpectrogramColor(double dblDecibel, double dblMinimumValue, double dblMaximumValue, int[] intInitialGradient, int[] intFinalGradient) {
    	int intRGB[] = new int[3];
    	
    	// Normaliza para valores entre 0 >= x <= 1
    	double dblNormalized = (double) (dblDecibel - dblMinimumValue) / (dblMaximumValue - dblMinimumValue);
    
    	// Cria um gradiente levando em consideração a cor inicial e final
		intRGB[0] = (int)((double) intInitialGradient[0] * (1.0d - dblNormalized) + (double) intFinalGradient[0] * dblNormalized); // Red
		intRGB[1] = (int)((double) intInitialGradient[1] * (1.0d - dblNormalized) + (double) intFinalGradient[1] * dblNormalized); // Green
		intRGB[2] = (int)((double) intInitialGradient[2] * (1.0d - dblNormalized) + (double) intFinalGradient[2] * dblNormalized); // Blue

		return intRGB;
    }

    /**
     * Retorna o número de caixas de frequência que 
     * serão utilizadas na renderização do espectrograma.
     * 
     * @param blnAllFrequencyFrames - Considera todos os frames de frequência
     * 
     * @return intNumberFrequencyFrames
     */
    private int getNumberFrequencyFrames(boolean blnAllFrequencyFrames) {
    	int intMaximumFrequency = objSpectrogram.getMaximumFrequency();
    	int intTotalFrequencyFrames = FFTParameters.getInstance().getFFTSampleSize() / 2;
    	
    	intInitialFrequencyFrame = 0;
    	intFinalFrequencyFrame = intTotalFrequencyFrames;
        
    	if (intRenderType == objSpectrogram.RENDER_DEFAULT && !blnAllFrequencyFrames) {
    		double dblFrequencyBin = (double) intMaximumFrequency / (double) intTotalFrequencyFrames; // Tamanho da caixa que cada frequência terá quando calculada a FFT (em Hertz)
        	
	        for (int indexFrequencyFrame = 1; indexFrequencyFrame <= intTotalFrequencyFrames + 1; indexFrequencyFrame++) {
	        	double dblCurrentFrequency = (double) (indexFrequencyFrame * dblFrequencyBin);
	        	
	        	// Frame da frequência inicial
	        	if (dblCurrentFrequency >= objSpectrogram.getInitialFrequency()) {
	        		if (intInitialFrequencyFrame == -1) {
	        			intInitialFrequencyFrame = indexFrequencyFrame;
	        		}
	        	}
	        	
	        	// Frame da frequência final
	        	if (dblCurrentFrequency >= objSpectrogram.getFinalFrequency()) {
	        		if (intFinalFrequencyFrame == intTotalFrequencyFrames) {
	        			intFinalFrequencyFrame = indexFrequencyFrame;
	        			break;
	        		}
	        	}
	        }
    	}

        int intNumberFrequencyFrames = intFinalFrequencyFrame - intInitialFrequencyFrame;
        
        return intNumberFrequencyFrames;
    }
}

/**
 * 
 * 
 * A classe implementa <i>Comparable</i>, pois através do método <i>compareTo</i>
 * é possível ordenar uma lista dessa classe através do campo <i>intInitialChunk</i>.
 * 
 * @author Leandro Tacioli
 * @version 1.0 - 08/Jan/2015
 */
class SpectrogramRendererIntensities implements Comparable<SpectrogramRendererIntensities> {
	private int intInitialChunk;
	private double[] intensities;

	public double[] getIntensities() {
		return intensities;
	}
	
	/**
	 * A classe implementa <i>Comparable</i>, pois através do método <i>compareTo</i>
	 * é possível ordenar uma lista dessa classe através do campo <i>intInitialChunk</i>.
	 * 
	 * @param intInitialChunk
	 * @param intensities
	 */
	protected SpectrogramRendererIntensities(int intInitialChunk, double[] intensities) {
		this.intInitialChunk = intInitialChunk;
		this.intensities = intensities;
	}

	@Override
	public int compareTo(SpectrogramRendererIntensities objIntensitiesToCompare) {
		if (this.intInitialChunk < objIntensitiesToCompare.intInitialChunk) {
            return -1;
        }
		
        if (this.intInitialChunk > objIntensitiesToCompare.intInitialChunk) {
            return 1;
        }
        
        return 0;
	}
}