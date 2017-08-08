package br.unicamp.fnjv.wasis.graphics.spectrogram;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.objectplanet.image.PngEncoder;

import br.unicamp.fnjv.wasis.audio.temporary.AudioTemporary;
import br.unicamp.fnjv.wasis.audio.temporary.AudioTemporaryImages;
import br.unicamp.fnjv.wasis.dsp.FFTParameters;
import br.unicamp.fnjv.wasis.features.PowerSpectrumValues;
import br.unicamp.fnjv.wasis.features.MFCC;
import br.unicamp.fnjv.wasis.libs.FileManager;
import br.unicamp.fnjv.wasis.main.WasisParameters;
import br.unicamp.fnjv.wasis.multimidia.wav.AudioWav;

/**
 * Processa o espectrograma de um arquivo de áudio.
 * 
 * @author Leandro Tacioli
 * @version 3.3 - 24/Fev/2017
 */
public class Spectrogram extends JPanel {
	private static final long serialVersionUID = -3247613272744461708L;

	private AudioWav objAudioWav;                    // Objeto do Áudio WAV
	
	private int intAudioTemporaryIndex;              // Índice do áudio temporário na lista da memória 
	
	private int intFFTSampleSize;                    // Número de amostras da FFT (Potência de 2)
    private int intFFTOverlapFactor;                 // Fator de sobreposição (OVERLAP)
    private String strFFTWindowFuntion;              // Função de janelamento
    
    private int intPanelWidth;                       // Comprimento do painel onde o espectrograma será desenhado
    private int intPanelHeight;                      // Altura do painel onde o espectrograma será desenhado
    
    private int intMaximumTime;                      // Tempo total (em milisegundos) do arquivo de áudio
    private int intInitialTime;                      // Tempo inicial (em milisegundos) que está sendo mostrado na tela (atualizado com zoom in/out)
    private int intFinalTime;                        // Tempo final (em milisegundos) que está sendo mostrado na tela (atualizado com zoom in/out)
    
	private int intInitialTimeOriginalImage;         // Tempo inicial da imagem original
	private int intFinalTimeOriginalImage;           // Tempo final da imagem original

    private int intInitialTimeTemporaryImage;        // Tempo inicial da imagem temporária
	private int intFinalTimeTemporaryImage;          // Tempo final da imagem temporária
	
    private int intMaximumFrequency;                 // Frequência máxima do arquivo de áudio
    private int intInitialFrequency;                 // Frequência inicial que está sendo mostrada na tela (atualizada com zoom in/out)
    private int intFinalFrequency;                   // Frequência final que está sendo mostrada na tela (atualizada com zoom in/out)
	
    private int intOffsetFactor;                     // Nível de ajuste do ruído de fundo da imagem
	
	private boolean blnIsRenderingSpectrogram;       // Status da renderização do espectrograma
	private boolean blnAllowRenderSpectrogram;       // Status de permissão para renderização do espectrograma
	
	private boolean blnIsGeneratingTemporaryImages;  // Status da geração das imagens temporárias
	private boolean blnAllowGenerateTemporaryImages; // Status de permissão para geração de novas imagens temporárias
	
	/**
	 * Status que determina se a imagem temporária terá um tamanho pequeno.<br>
	 * <br>
	 * A constante <i>TIME_SHORT_TEMPORARY_IMAGE</i> determina o tempo máximo para
	 * a que a condição será verdadeira.<br>
	 * <br>
	 * <i>True</i> - Tamanho pequeno<br>
	 * <i>False</i> - Tamanho grande
	 */
	private boolean blnIsShortTemporaryImage;
	
    /**
     * Tempo máximo que a imagem temporária de tamanho pequeno poderá ter.
     */
    private final int TIME_SHORT_TEMPORARY_IMAGE = 60000;     // 60000 milisegundos = 60 segundos
	
	/**
	 * Imagem original do espectrograma (gerada na renderização)
	 */
	private BufferedImage spectrogramOriginalImage;
	
	/**
	 * Imagem final do espectrograma (considerando um <i>resize</i> para se ajustar ao painel)
	 */
	private BufferedImage spectrogramFinalImage;
	
	/**
	 * Imagem temporária do espectrograma (para agilizar o carregamento)
	 */
	private BufferedImage spectrogramTemporaryImage;
	
	/**
	 * Renderização padrão do espectrograma.
	 */
	protected final int RENDER_DEFAULT = 0;
	
	/**
	 * Renderização para imagens temporárias do espectrograma.
	 */
	protected final int RENDER_TEMPORARY = 1;
	
	/**
	 * Renderização para extração de dados para comparação e/ou gravação no banco de dados.
	 */
	protected final int RENDER_COMPARISON = 2;
    
    /**
     * Determina a taxa de zoom in/out que será aplicada à imagem (em porcentagem).
     */
    private final int ZOOM_RATE = 25;
    
    /**
	 * Retorna o objeto <i>AudioWav</i>.
	 * 
	 * @return objAudioWav
	 */
	public AudioWav getAudioWav() {
		return objAudioWav;
	}
	
	/**
	 * Retorna o índice do áudio temporário na lista da memória.
	 * 
	 * @return intAudioTemporaryIndex
	 */
	public int getAudioTemporaryIndex() {
		return intAudioTemporaryIndex;
	}

    /**
	 * Retorna o comprimento do painel onde o espectrograma será desenhado.
	 * 
	 * @return intPainelWidth
	 */
	protected int getPanelWidth() {
		return intPanelWidth;
	}
    
	/**
	 * Altera o comprimento do painel onde o espectrograma será desenhado.
	 * 
	 * @param intPainelWidth
	 */
	public void setPanelWidth(int intPanelWidth) {
		this.intPanelWidth = intPanelWidth;
	}
	
	/**
	 * Retorna a altura do painel onde o espectrograma será desenhado.
	 * 
	 * @return intPanelHeight
	 */
	protected int getPanelHeight() {
		return intPanelHeight;
	}

	/**
	 * Altera a altura do painel onde o espectrograma será desenhado.
	 * 
	 * @param intPanelHeight
	 */
	public void setPanelHeight(int intPanelHeight) {
		this.intPanelHeight = intPanelHeight;
	}
	
	/**
	 * Retorna o tempo inicial que está sendo mostrado na imagem do espectrograma.
	 * 
	 * @return intInitialTime
	 */
	public int getInitialTime() {
		return intInitialTime;
	}
	
	/**
	 * Retorna o tempo final que está sendo mostrado na imagem do espectrograma.
	 * 
	 * @return intFinalTime
	 */
	public int getFinalTime() {
		return intFinalTime;
	}
	
	/**
	 * Retorna a frequência inicial que está sendo mostrada na imagem do espectrograma.
	 * 
	 * @return intInitialFrequency
	 */
	public int getInitialFrequency() {
		return intInitialFrequency;
	}
	
	/**
	 * Retorna a frequência final que está sendo mostrada na imagem do espectrograma.
	 * 
	 * @return intFinalFrequency
	 */
	public int getFinalFrequency() {
		return intFinalFrequency;
	}
	
	/**
	 * Retorna a frequência máxima do áudio.
	 * 
	 * @return intMaximumFrequency
	 */
	public int getMaximumFrequency() {
		return intMaximumFrequency;
	}
	
	/**
	 * Retorna o nível de ajuste do ruído de fundo da imagem.
	 * 
	 * @return intOffsetFactor
	 */
	public int getOffsetFactor() {
		return intOffsetFactor;
	}
	
    /**
     * Ajusta o fator que calcula os valores da escalas de cinza das intensidades.
     * Basicamente utilizado para ajustar o nível de ruído de fundo da imagem.
     *
     * @param intOffsetFactor
     */
    public void setOffsetFactor(int intOffsetFactor) {
        this.intOffsetFactor = intOffsetFactor;
    }
    
    /**
	 * Retorna o status de renderização do espectrograma. <br>
	 * <i>True</i> - Em renderização <br>
	 * <i>False</i> - Já renderizado
	 * 
	 * @return blnIsRenderingSpectrogram
	 */
	public boolean getIsRenderingSpectrogram() {
		return blnIsRenderingSpectrogram;
	}
	
	/**
	 * Retorna o status de permissão para renderização do espectrograma. <br>
	 * <i>True</i> - Permitido <br>
	 * <i>False</i> - Bloqueado
	 * 
	 * @return blnAllowRenderSpectrogram
	 */
	public boolean getAllowRenderSpectrogram() {
		return blnAllowRenderSpectrogram;
	}
	
	/**
	 * Retorna o status que determina se a imagem temporária tem o tamanho pequeno.
	 * 
	 * @return blnIsShortTemporaryImage
	 */
	public boolean getIsShortTemporaryImage() {
		return blnIsShortTemporaryImage;
	}
	
	/**
	 * Retorna a imagem final do espectrograma.
	 * 
	 * @return spectrogramFinalImage
	 */
	protected BufferedImage getSpectrogramFinalImage() {
		return spectrogramFinalImage;
	}
	
	/**
     * Processa o espectrograma de um arquivo de áudio.
     * 
     * @param objAudioWav - Objeto do áudio WAV
     */
	public Spectrogram(AudioWav objAudioWav) {
		this.objAudioWav = objAudioWav;
		
		this.intAudioTemporaryIndex = AudioTemporary.getAudioTemporaryIndex(objAudioWav);
		
        this.intMaximumTime = objAudioWav.getTotalTime();
        this.intInitialTime = 0;
        this.intFinalTime = intMaximumTime;
        
        this.intMaximumFrequency = objAudioWav.getWavHeader().getSampleRate() / 2;  // É dividido por 2 (Teorema Nyquist-Shannon)
        this.intInitialFrequency = 0;
        this.intFinalFrequency = intMaximumFrequency;
        
        this.blnAllowRenderSpectrogram = true;

        // Tamanho da imagem temporária é pequeno
        if (intMaximumTime <= TIME_SHORT_TEMPORARY_IMAGE) {
        	this.blnIsShortTemporaryImage = true;  
        }
	}
	
	/**
     * Renderiza o espectrograma.
     */
    public void renderSpectrogram() {
    	try {
	    	blnIsRenderingSpectrogram = true;
	    	
	    	// Verifica se houve uma mudança nos parâmetros do FFT
	    	if (intFFTSampleSize != FFTParameters.getInstance().getFFTSampleSize() || 
	    			intFFTOverlapFactor != FFTParameters.getInstance().getFFTOverlapFactor() || 
	    				strFFTWindowFuntion != FFTParameters.getInstance().getFFTWindowFunction()) {
	    		
	    		intFFTSampleSize = FFTParameters.getInstance().getFFTSampleSize();
	    		intFFTOverlapFactor = FFTParameters.getInstance().getFFTOverlapFactor();
	    		strFFTWindowFuntion = FFTParameters.getInstance().getFFTWindowFunction();
	    		
	    		intInitialTimeTemporaryImage = 0;
	    		intFinalTimeTemporaryImage = 0;
	    		
	    		spectrogramTemporaryImage = null;
	    		
	    		blnAllowRenderSpectrogram = true;         // Permite novamente a renderização do espectrograma com os novos parâmetros
	    		blnAllowGenerateTemporaryImages = false;  // Bloqueia a criação das imagens temporárias que estiverem em progresso (se houver)
	    	}
	    	
	    	// Verifica se há alguma imagem temporária para auxiliar no carregamento
    		checkExistingTemporaryImage();
    		
    		// Caso a imagem temporária existente compreenda toda a extensão do áudio, não é necessário realizar a renderização
    		if (intInitialTimeTemporaryImage == 0 && intFinalTimeTemporaryImage == intMaximumTime) {
    			blnAllowRenderSpectrogram = false;
    		}
	    	
	    	// Permite a renderização do espectrograma
	    	if (blnAllowRenderSpectrogram) {
		    	SpectrogramRenderer objSpectrogramRenderer = new SpectrogramRenderer(this, RENDER_DEFAULT);
		    	objSpectrogramRenderer.executeRenderer(intInitialTime, intFinalTime);
		    	
		    	intInitialTimeOriginalImage = intInitialTime;
		    	intFinalTimeOriginalImage = intFinalTime;
		    	
		    	spectrogramOriginalImage = objSpectrogramRenderer.getSpectrogramImage(false);
		    	
		    	scaleFinalImage(spectrogramOriginalImage, intPanelWidth, intPanelHeight);
		    	
		    	// Caso seja gerada uma imagem temporária de tamanho pequeno, 
		    	// executa novamente a renderização com o tempo inicial e final do áudio
		    	// Será util no caso de haver um zoom na imagem e algum parâmetro da FFT for alterado
		    	if (blnIsShortTemporaryImage) {
		    		if (intInitialTime != 0 || intFinalTime != intMaximumTime) {
		    			
		    			// ************** Fazer a verificar se já existe a imagem ********************
		    			// ************* Se não fizer tudo bem, mas vai carregar mais devagar *************
		    			
				    	objSpectrogramRenderer.executeRenderer(0, intMaximumTime);
				    	
				    	intInitialTimeOriginalImage = 0;
				    	intFinalTimeOriginalImage = intMaximumTime;
				    	
				    	spectrogramOriginalImage = objSpectrogramRenderer.getSpectrogramImage(true);
				    	
				    	reloadSpectrogramFromOriginalImage();
		    		}
		    		
		    		// Gera a imagem temporária quando somente uma for necessária
			    	// Não é gerada em background, pois a imagem 'spectrogramImageTemporary' 
			    	// será necessária para auxiliar em todos os carregamentos
		    		generateTemporaryImageFile(spectrogramOriginalImage, 0, intMaximumTime);
		    		
		    	// Caso seja gerada uma imagem temporária de tamanho grande, 
				// gera imagens temporárias do espectrograma em background
				} else {
					generateTemporaryImage();
				}
	    	}
    	
    	} catch (Exception e) {
    		e.printStackTrace();
    		
    	} finally {
    		blnAllowRenderSpectrogram = false;
    		blnIsRenderingSpectrogram = false;
    	}
    }
    
    /**
     * Verifica se já existe alguma imagem temporária na lista da memória (<i>AudioTemporary.getAudioTemporary()</i>).
     */
    private void checkExistingTemporaryImage() {
    	List<AudioTemporaryImages> lstAudioTemporaryImages = AudioTemporary.getAudioTemporary().get(intAudioTemporaryIndex).getAudioTemporaryImages();
    	
    	try {
			// Loop através das imagens do arquivo temporário
			for (int indexImages = 0; indexImages < lstAudioTemporaryImages.size(); indexImages++) {
				// É necessário verificar também se os parâmetros da FFT em processamento é igual das imagens temporárias
				if (intFFTSampleSize == lstAudioTemporaryImages.get(indexImages).getFFTSamples() &&
						intFFTOverlapFactor == lstAudioTemporaryImages.get(indexImages).getFFTOverlap() && 
							strFFTWindowFuntion.equals(lstAudioTemporaryImages.get(indexImages).getFFTWindow())) {
					
					String strSpectrogramImagePath = lstAudioTemporaryImages.get(indexImages).getSpectrogramImagePath();
					String strSpectrogramImageHash = lstAudioTemporaryImages.get(indexImages).getSpectrogramImageHash();
					
					File fileSpectrogram = new File(strSpectrogramImagePath);
					String strSpectrogramImageHashMemory = FileManager.getFileHash(fileSpectrogram);
					
					// Verifica se o arquivo existente na pasta temporária é o mesmo que está na lista da memória
					if (!strSpectrogramImageHash.equals(strSpectrogramImageHashMemory)) {
						break;
					}
					
					intInitialTimeTemporaryImage = lstAudioTemporaryImages.get(indexImages).getInitialTime();
					intFinalTimeTemporaryImage = lstAudioTemporaryImages.get(indexImages).getFinalTime();
					
					spectrogramTemporaryImage = ImageIO.read(fileSpectrogram);
					
					// Caso a imagem temporária tenha tamanho pequeno
					if (blnIsShortTemporaryImage) {
				    	intInitialTimeOriginalImage = intInitialTimeTemporaryImage;
				    	intFinalTimeOriginalImage = intFinalTimeTemporaryImage;
				    	
				    	spectrogramOriginalImage = spectrogramTemporaryImage;
				    	
				    	scaleFinalImage(spectrogramOriginalImage, intPanelWidth, intPanelHeight);
				    	
				    // Imagem temporária de tamanho grande
					} else {
						if (intInitialTime == intInitialTimeTemporaryImage && intMaximumTime == intFinalTimeTemporaryImage) {
							intInitialTimeOriginalImage = intInitialTime;
					    	intFinalTimeOriginalImage = intMaximumTime;
					    	
							spectrogramOriginalImage = spectrogramTemporaryImage;
							
							scaleFinalImage(spectrogramOriginalImage, intPanelWidth, intPanelHeight);
						}
					}
				}
			}
	    	
    	} catch (Exception e) {
			intInitialTimeTemporaryImage = 0;
			intFinalTimeTemporaryImage = 0;

			intInitialTimeOriginalImage = 0;
			intFinalTimeOriginalImage = 0;
			
			spectrogramTemporaryImage = null;
			spectrogramOriginalImage = null;
			spectrogramFinalImage = null;
			
			e.printStackTrace();
    	}
    }
    
    /**
     * Gera imagens temporárias do espectrograma em background.<br>
     * <br>
     * Essas imagens auxiliarão nos futuros carregamentos do espectrograma,
     * realizando um processo mais rápido.
     */
	private void generateTemporaryImage() {
		if (!blnIsGeneratingTemporaryImages && !blnIsShortTemporaryImage) {
			blnAllowGenerateTemporaryImages = true;
			blnIsGeneratingTemporaryImages = true;
			
	    	SwingWorker<Void, Void> swingWorkerGenerateImages = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					try {
						int intInitialTime = intFinalTimeTemporaryImage;     // Tempo inicial a ser processado
						int intFinalTime = 0;                                // Tempo final a ser processado
						
						// Adiciona '1' para iniciar na posição após o final da imagem temporária já existente
						if (intInitialTime != 0) {
							intInitialTime = intInitialTime + 1;
						}
				
						int intRemainingTime = intMaximumTime - intInitialTime;    // Tempo restante a ser processado
						
						// A imagem temporária será gerada por partes equivalentes a 'TIME_SHORT_TEMPORARY_IMAGE'
						while (intInitialTime < intMaximumTime && intRemainingTime > 0 && blnAllowGenerateTemporaryImages) {
							intFinalTime = intInitialTime + TIME_SHORT_TEMPORARY_IMAGE - 1;
							
							if (intFinalTime <= intMaximumTime) {
								// Imagem inicial ou intermediária
								if (intFinalTime <= intMaximumTime) {
									renderSpectrogramTemporaryImage(intInitialTime, intFinalTime);
									
									intInitialTime = intInitialTime + TIME_SHORT_TEMPORARY_IMAGE;
									intRemainingTime = intRemainingTime - TIME_SHORT_TEMPORARY_IMAGE;
									
								// Imagem final
								} else if (intFinalTime > intMaximumTime) {
									renderSpectrogramTemporaryImage(intInitialTime, intMaximumTime);
									
									intRemainingTime = 0;
								}
								
							// Última imagem a ser gerada
							} else {
								renderSpectrogramTemporaryImage(intInitialTime, (intInitialTime + intRemainingTime));
								
								intRemainingTime = 0;
							}
						}
						
						if (intInitialTime < intMaximumTime && intRemainingTime > 0 && !blnAllowGenerateTemporaryImages) {
							if (spectrogramTemporaryImage != null && intFinalTimeTemporaryImage != 0) {
								saveTemporaryImage(0, intFinalTimeTemporaryImage, false);
							}
						}
					
						blnAllowGenerateTemporaryImages = false;
						blnIsGeneratingTemporaryImages = false;
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					return null;
				}
			};
			
			swingWorkerGenerateImages.execute();
		}
    }
    
    /**
     * Renderiza as imagens temporárias do espectrograma que está sendo executado em background.
     * 
     * @param intInitialTime - Tempo inicial
     * @param intFinalTime   - Tempo final
     */
    private void renderSpectrogramTemporaryImage(int intInitialTime, int intFinalTime) {
    	if (blnAllowGenerateTemporaryImages) {
    		SpectrogramRenderer objSpectrogramRenderer = new SpectrogramRenderer(this, RENDER_TEMPORARY);
	    	objSpectrogramRenderer.executeRenderer(intInitialTime, intFinalTime);
	    	
	    	BufferedImage imageTemporary = objSpectrogramRenderer.getSpectrogramImage(true);
	    	
	    	generateTemporaryImageFile(imageTemporary, intInitialTime, intFinalTime);
    	}
    }
    
    /**
     * Gera o arquivo PNG da imagem temporária.<br>
     * <br>
     * A imagem temporária atual (<i>spectrogramImageTemporary</i>) 
     * é concatenada com uma nova imagem.<br>
     * <br>
     * <i>spectrogramImageTemporary</i> será o resultado final da concatenação.
     * 
     * @param imageTemporary - Pedaço da imagem temporária a ser adicionada a final
     * @param intInitialTime - Tempo inicial a ser adicionado a imagem
     * @param intFinalTime   - Tempo final a ser adicionado a imagem
     */
    private void generateTemporaryImageFile(BufferedImage imageTemporary, int intInitialTime, int intFinalTime) {
    	try {
    		BufferedImage mergedImage = null;
        	
        	// Caso seja o início da imagem temporária
    		if (intInitialTime == 0) {
    			mergedImage = imageTemporary;
    		
    		// Caso a imagem temporária já tem parte do áudio carregado - concatena as imagens
    		} else {
    	    	int intWidth = spectrogramTemporaryImage.getWidth() + imageTemporary.getWidth();
    	    	int intHeight = Math.max(spectrogramTemporaryImage.getHeight(), imageTemporary.getHeight());
    	    	
    	    	mergedImage = new BufferedImage(intWidth, intHeight, BufferedImage.TYPE_INT_ARGB);
    	    	
    	    	Graphics2D g2 = mergedImage.createGraphics();
    	    	g2.drawImage(spectrogramTemporaryImage, null, 0, 0);
    	    	g2.drawImage(imageTemporary, null, spectrogramTemporaryImage.getWidth(), 0);
    	    	g2.dispose();
    		}
    		
    		spectrogramTemporaryImage = mergedImage;
    		
    		intInitialTimeTemporaryImage = 0;
    		intFinalTimeTemporaryImage = intFinalTime;
    		
    		// Salva os dados da imagem temporária apenas quando chegar ao final da imagem
    		if (intFinalTime == intMaximumTime) {
    			saveTemporaryImage(0, intFinalTime, true);
    		}
    		
    	} catch (Exception e) {
    		intInitialTimeTemporaryImage = 0;
    		intFinalTimeTemporaryImage = 0;
    		
    		spectrogramTemporaryImage = null;
    		
			e.printStackTrace();
		}
    }
    
    /**
     * Salva a imagem temporária do espectrograma.
     * 
     * @param intInitialTime   - Tempo inicial da imagem
     * @param intFinalTime     - Tempo final da imagem
     * @param blnCompleteImage - Determina se é a imagem completa do espectrograma
     */
    private void saveTemporaryImage(int intInitialTime, int intFinalTime, boolean blnCompleteImage) {
    	try {
    		String strSpectrogramImagePath = WasisParameters.getInstance().TEMPORARY_FOLDER + objAudioWav.getAudioFileNameTemporary() + "-" + intFFTSampleSize + "-" + intFFTOverlapFactor + "-" + strFFTWindowFuntion + ".png";
			
    		File fileSpectrogramImagePath = new File(strSpectrogramImagePath);

    		OutputStream outputStream = new FileOutputStream(fileSpectrogramImagePath);
	    	
    		PngEncoder objPngEncoder = new PngEncoder();
	    	objPngEncoder.encode(spectrogramTemporaryImage, outputStream);
	    	
	    	outputStream.flush();
	    	outputStream.close();
	    	
	    	String strSpectrogramImageHash = FileManager.getFileHash(fileSpectrogramImagePath);
	    	
	    	AudioTemporary.createAudioTemporaryImage(objAudioWav, strSpectrogramImagePath, strSpectrogramImageHash, intFFTSampleSize, intFFTOverlapFactor, strFFTWindowFuntion, intInitialTime, intFinalTime, blnCompleteImage);
	    	
	    	if (intInitialTime == 0 && intFinalTime == intMaximumTime) {
	    		spectrogramOriginalImage = spectrogramTemporaryImage;
	    		
	    		intInitialTimeTemporaryImage = 0;
	    		intFinalTimeOriginalImage = intMaximumTime;
	    	}
	    	
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    /**
     * Recarrega o espectrograma baseando-se na seleção de parte do áudio.
     * 
     * @param intInitialTime - Tempo inicial
     * @param intTimeFinal   - Tempo final
     */
    public void reloadSpectrogramByTimeSelection(int intInitialTime, int intFinalTime) {
    	this.intInitialTime = intInitialTime;
    	this.intFinalTime = intFinalTime;
    	
    	reloadSpectrogram();
    }
    
    /**
     * Recarrega o espectrograma baseando-se na seleção de parte do áudio.
     * 
     * @param intInitialFrequency - Frequência inicial
     * @param intFrequencyFinal   - Frequência final
     */
    public void reloadSpectrogramByFrequencySelection(int intInitialFrequency, int intFinalFrequency) {
    	this.intInitialFrequency = intInitialFrequency;
    	this.intFinalFrequency = intFinalFrequency;
    	
    	reloadSpectrogramFromOriginalImage();
    }
    
    /**
     * Amplia o zoom no eixo do tempo.
     */
    public void setTimeZoomIn() {
    	try {
	    	int intTimeDifference = intFinalTime - intInitialTime;  // Diferença entre o valor inicial e final que o zoom se encontra
	    	int intTimeValueZoomIn = (int) (intTimeDifference * (float) (ZOOM_RATE / 100f));
	    	
	    	intFinalTime = intFinalTime - intTimeValueZoomIn;
	    	
	    	reloadSpectrogram();
	    	
    	} catch (Exception e) {
    		// Não é mais possível
    	}
    }
    
    /**
     * Reduz o zoom no eixo do tempo.
     */
    public void setTimeZoomOut() {
    	int intTimeDifference = intFinalTime - intInitialTime;  // Diferença entre o valor inicial e final que o zoom se encontra
    	int intValueZoomOut = (int) (intTimeDifference * (float) (ZOOM_RATE / 100f));
    	
    	if (intInitialTime > 0) {
    		intInitialTime = intInitialTime - (intValueZoomOut / 2);
    		intFinalTime = intFinalTime + (intValueZoomOut / 2);
    	} else {
    		intFinalTime = intFinalTime + intValueZoomOut;
    	}
    	
    	// Quando o valor for muito baixo, faz uma verificação para ver se realmente alterou os valores
    	if (intTimeDifference == (intFinalTime - intInitialTime)) {
    		intInitialTime = intInitialTime - 1;
    		intFinalTime = intFinalTime + 1;
    	}

    	// Tempo inicial não pode ser menor que o tempo mínimo do áudio
    	if (intInitialTime < 0) {
    		intInitialTime = 0;
    	}
    	
    	// Tempo final não pode ultrapassar o tempo máximo do áudio
    	if (intFinalTime > intMaximumTime) {
    		intFinalTime = intMaximumTime;
    	}
    	
    	reloadSpectrogram();
    }
    
    /**
     * Reseta o zoom no eixo do tempo.
     */
    public void setTimeZoomReset() {
    	if (intInitialTime != 0 || intFinalTime != intMaximumTime) {
	    	intInitialTime = 0;
	    	intFinalTime = intMaximumTime;
	    	
	    	reloadSpectrogram();
    	}
    }
    
    /**
     * Amplia o zoom no eixo da frequência.
     */
    public void setFrequencyZoomIn() {
    	int intFrequencyDifference = intFinalFrequency - intInitialFrequency;  // Diferença entre o valor inicial e final que o zoom se encontra
    	int intValueZoomIn = (int) (intFrequencyDifference * (float) (ZOOM_RATE / 100f));

		intFinalFrequency = intFinalFrequency - intValueZoomIn;
		
		reloadSpectrogramFromOriginalImage();
    }
    
    /**
     * Reduz o zoom no eixo da frequência.
     */
    public void setFrequencyZoomOut() {
    	int intFrequencyDifference = intFinalFrequency - intInitialFrequency;  // Diferença entre o valor inicial e final que o zoom se encontra
    	int intValueZoomOut = (int) (intFrequencyDifference * (float) (ZOOM_RATE / 100f));

    	intFinalFrequency = intFinalFrequency + intValueZoomOut;
    	
    	if (intInitialFrequency > 0) {
    		intInitialFrequency = intInitialFrequency - (intValueZoomOut / 2);
    		intFinalFrequency = intFinalFrequency + (intValueZoomOut / 2);
    	} else {
    		intFinalFrequency = intFinalFrequency + intValueZoomOut;
    	}
    	
    	// Quando o valor for muito baixo, faz uma verificação para ver se realmente alterou os valores
    	if (intFrequencyDifference == (intFinalFrequency - intInitialFrequency)) {
    		intInitialFrequency = intInitialFrequency - 1;
    		intFinalFrequency = intFinalFrequency + 1;
    	}

    	// Frequência inicial não pode ser menor que a frequência mínima do áudio
    	if (intInitialFrequency < 0) {
    		intInitialFrequency = 0;
    	}
    	
    	// Frequência final não pode ultrapassar a frequência mínima do áudio
    	if (intFinalFrequency > intMaximumFrequency) {
    		intFinalFrequency = intMaximumFrequency;
    	}

    	reloadSpectrogramFromOriginalImage();
    }
    
    /**
     * Reseta o zoom no eixo da frequência.
     */
    public void setFrequencyZoomReset() {
    	if (intInitialFrequency != 0 || intFinalFrequency != intMaximumFrequency) {
	    	intInitialFrequency = 0;
	    	intFinalFrequency = intMaximumFrequency;
	    	
	    	reloadSpectrogramFromOriginalImage();
    	}
    }
    
    /**
     * Executa o zoom baseando em um tempo e frequência inicial e final.
     * 
     * @param intInitialTime      - Tempo inicial
     * @param intFinalTime        - Tempo final
     * @param intInitialFrequency - Frequência inicial
     * @param intFinalFrequency   - Frequência final
     */
    public void setZoomSelection(int intInitialTime, int intFinalTime, int intInitialFrequency, int intFinalFrequency) {
    	this.intInitialTime = intInitialTime;
    	this.intFinalTime = intFinalTime;
    	this.intInitialFrequency = intInitialFrequency;
    	this.intFinalFrequency = intFinalFrequency;
    	
    	reloadSpectrogram();
    }

    /**
     * Recarrega o espectrograma.
     */
    private void reloadSpectrogram() {
    	// Caso a imagem temporária tenha o tamanho pequeno
    	if (blnIsShortTemporaryImage) {
    		reloadSpectrogramFromCurrentTemporaryImage();
    		
    	// Caso a imagem temporária tenha o tamanho grande
    	} else {
    		
    		// Se já houver uma imagem temporária carregada no buffer, verifica se os tempos iniciais e 
    		// finais solicitados estão dentro dos tempos inicias e finais da imagem temporária.
    		// Nesse caso, carrega o espectrograma através da imagem temporária
    		if (intInitialTime >= intInitialTimeTemporaryImage && intFinalTime <= intFinalTimeTemporaryImage) {
    			reloadSpectrogramFromCurrentTemporaryImage();
    			
    		// Senão renderiza novamente o espectrograma
    		} else {
    			blnAllowRenderSpectrogram = true;
    			//renderSpectrogram();
    			//reloadSpectrogramFromExistingTemporaryImages();
    		}
    	}
    }
    
    /**
     * Recarrega o espectrograma a partir da imagem original (gerada na renderização).
     */
    private void reloadSpectrogramFromOriginalImage() {
    	BufferedImage spectrogramImageToReload = spectrogramOriginalImage;
    	
    	// ******************************************************************************
    	// Tempo
    	double dblTimePerPixel = (double) (intFinalTimeOriginalImage - intInitialTimeOriginalImage) / spectrogramImageToReload.getWidth();
    	
    	// Pega a posição do pixel do tempo inicial
    	int intInitialTimePixel = (int) ((intInitialTime / dblTimePerPixel) - (intInitialTimeOriginalImage / dblTimePerPixel));
    	
    	if (intInitialTimePixel < 0) {
    		intInitialTimePixel = 0;
    	}
    	
    	// Pega a posição do pixel do tempo final
    	int intFinalTimePixel = (int) (spectrogramImageToReload.getWidth() - ((intFinalTimeOriginalImage / dblTimePerPixel) - (intFinalTime / dblTimePerPixel)));
    	
    	if (intFinalTimePixel > spectrogramImageToReload.getWidth()) {
    		intFinalTimePixel = spectrogramImageToReload.getWidth();
    	}
    	
    	intFinalTimePixel = intFinalTimePixel - intInitialTimePixel;
    	
    	if (intFinalTimePixel <= 0) {
    		intFinalTimePixel = 1;
    	}
    	
    	// ******************************************************************************
    	// Frequência
    	double dblFrequencyPerPixel = (double) intMaximumFrequency / spectrogramImageToReload.getHeight();

    	// Pega a posição do pixel da frequência inicial
    	int intInitialFrequencyPixel = (int) ((intMaximumFrequency / dblFrequencyPerPixel) - (intInitialFrequency / dblFrequencyPerPixel));

    	if (intInitialFrequencyPixel > spectrogramImageToReload.getHeight()) {
    		intInitialFrequencyPixel = spectrogramImageToReload.getHeight();
    	}
    	
    	// Pega a posição do pixel da frequência final
    	int intFinalFrequencyPixel = (int) ((intMaximumFrequency / dblFrequencyPerPixel) - (intFinalFrequency / dblFrequencyPerPixel));
    	
    	if (intFinalFrequencyPixel < 0) {
    		intFinalFrequencyPixel = 0;
    	}
    	
    	intInitialFrequencyPixel = intInitialFrequencyPixel - intFinalFrequencyPixel;
    	
    	// Os valores de frequência inicial e final são invertidos, pois a frequência é mostrada de baixo para cima
    	spectrogramImageToReload = spectrogramImageToReload.getSubimage(intInitialTimePixel, intFinalFrequencyPixel, intFinalTimePixel, intInitialFrequencyPixel);
    	
    	scaleFinalImage(spectrogramImageToReload, intPanelWidth, intPanelHeight);
    }

    /**
     * Recarrega o espectrograma a partir de uma imagem temporária.<br>
     * <br>
     * Caso não haja uma imagem temporária, o espectrograma deve ser renderizado novamente.
     */
    private void reloadSpectrogramFromCurrentTemporaryImage() {
    	BufferedImage spectrogramImageToReload = spectrogramTemporaryImage;
    	
    	// ******************************************************************************
    	// Tempo
    	double dblTimePerPixel = (double) (intFinalTimeTemporaryImage - intInitialTimeTemporaryImage) / spectrogramImageToReload.getWidth();
    	
    	// Pega a posição do pixel do tempo inicial
    	int intInitialTimePixel = (int) ((intInitialTime / dblTimePerPixel) - (intInitialTimeTemporaryImage / dblTimePerPixel));
    	
    	if (intInitialTimePixel < 0) {
    		intInitialTimePixel = 0;
    	}
    	
    	// Pega a posição do pixel do tempo final
    	int intFinalTimePixel = (int) (spectrogramImageToReload.getWidth() - ((intFinalTimeTemporaryImage / dblTimePerPixel) - (intFinalTime / dblTimePerPixel)));
    	
    	if (intFinalTimePixel > spectrogramImageToReload.getWidth()) {
    		intFinalTimePixel = spectrogramImageToReload.getWidth();
    	}
    	
    	intFinalTimePixel = intFinalTimePixel - intInitialTimePixel;
    	
    	if (intFinalTimePixel <= 0) {
    		intFinalTimePixel = 1;
    	}
    	
    	// ******************************************************************************
    	// Frequência
    	double dblFrequencyPerPixel = (double) intMaximumFrequency / spectrogramImageToReload.getHeight();

    	// Pega a posição do pixel da frequência inicial
    	int intInitialFrequencyPixel = (int) ((intMaximumFrequency / dblFrequencyPerPixel) - (intInitialFrequency / dblFrequencyPerPixel));

    	if (intInitialFrequencyPixel > spectrogramImageToReload.getHeight()) {
    		intInitialFrequencyPixel = spectrogramImageToReload.getHeight();
    	}
    	
    	// Pega a posição do pixel da frequência final
    	int intFinalFrequencyPixel = (int) ((intMaximumFrequency / dblFrequencyPerPixel) - (intFinalFrequency / dblFrequencyPerPixel));
    	
    	if (intFinalFrequencyPixel < 0) {
    		intFinalFrequencyPixel = 0;
    	}
    	
    	intInitialFrequencyPixel = intInitialFrequencyPixel - intFinalFrequencyPixel;

    	// Os valores de frequência inicial e final são invertidos, pois a frequência é mostrada de baixo para cima
    	spectrogramImageToReload = spectrogramImageToReload.getSubimage(intInitialTimePixel, intFinalFrequencyPixel, intFinalTimePixel, intInitialFrequencyPixel);

    	scaleFinalImage(spectrogramImageToReload, intPanelWidth, intPanelHeight);
    }

    /**
     * Ajusta o tamanho do espectrograma final no painel.<br>
     * <br>
     * A variável <i>spectrogramImageFinal</i> é alterada para
     * o tamanho final.
     * 
     * @param spectrogramImageToScale - Imagem do espectrograma a ser ajustada
     * @param intImageWidth           - Comprimento final da imagem
     * @param intImageHeight          - Altura final da imagem
     */
    protected void scaleFinalImage(final BufferedImage spectrogramImageToScale, int intImageWidth, int intImageHeight) {
    	BufferedImage imageToScale = new BufferedImage(intImageWidth, intImageHeight, BufferedImage.TYPE_INT_RGB);
    	
        final Graphics2D graphics2D = imageToScale.createGraphics();
        graphics2D.setComposite(AlphaComposite.Src);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.drawImage(spectrogramImageToScale, 0, 0, intImageWidth, intImageHeight, null);
        graphics2D.dispose();
        
        spectrogramFinalImage = imageToScale;
    }
    
    /**
     * Retorna os dados utilizados pelo sistema para realização de comparação de dados. <br>
     * <br>
     * Os dados são correspondentes à uma lista que contém os dados de frequência e intensidade máxima (decibel).
     * 
     * @param intInitialTime      - Tempo inicial a partir do qual serão retornados os dados
     * @param intFinalTime        - Tempo final a partir do qual serão retornados os dados
     * @param intInitialFrequency - Frequência inicial a partir da qual serão retornados os dados
     * @param intFinalFrequency   - Frequência final a partir da qual serão retornados os dados
     * 
     * @return lstAudioComparisonValues
     */
    public List<PowerSpectrumValues> extractComparisonData(int intInitialTime, int intFinalTime, int intInitialFrequency, int intFinalFrequency) {
    	SpectrogramRenderer objSpectrogramRenderer = new SpectrogramRenderer(this, RENDER_COMPARISON);
    	objSpectrogramRenderer.executeRenderer(intInitialTime, intFinalTime);
    	
    	List<PowerSpectrumValues> lstAudioComparisonValues = objSpectrogramRenderer.getAudioComparisonValues();
    	
    	// Exclui da lista os valores que não estão entre a frequência inicial e final (exclui do final para o começo da lista)
    	int intFrequencySamples = FFTParameters.FFT_SAMPLE_SIZE_COMPARISON / 2;
        int intMargin = (int) ((float) (intMaximumFrequency / (float) intFrequencySamples)); // Margem para pegar uma amostra inferior e uma superior
        
        for (int indexComparisonValue = lstAudioComparisonValues.size() - 1; indexComparisonValue >= 0; indexComparisonValue--) {
        	if (lstAudioComparisonValues.get(indexComparisonValue).getFrequency() > intFinalFrequency + intMargin) {
        		lstAudioComparisonValues.remove(indexComparisonValue);
        	} else if (lstAudioComparisonValues.get(indexComparisonValue).getFrequency() < intInitialFrequency - intMargin) {
        		lstAudioComparisonValues.remove(indexComparisonValue);
        	}
        }
                
    	return lstAudioComparisonValues;
    }
    
    public double[][] extractComparisonDataMFCC(int intInitialTime, int intFinalTime, int intInitialFrequency, int intFinalFrequency) {
    	// IMPORTANTE: intInitialFrequency & intFinalFrequency são ignoradas pois MFCCs trata o frame inteiro
    	int intInitialChunkToProcess = getAudioWav().getSampleFromTime(intInitialTime);
		int intFinalChunkToProcess = getAudioWav().getSampleFromTime(intFinalTime);
		
		double[] arrayAmplitudes = getAudioWav().getAmplitudesChunk(1, intInitialChunkToProcess, intFinalChunkToProcess);
		
		MFCC objMFCC = new MFCC();
		objMFCC.process(arrayAmplitudes, getAudioWav().getWavHeader().getSampleRate());
		
		double[][] mfccVector = objMFCC.getMFCC();
		double[] mean = objMFCC.getMean();
		double[] standardDeviation = objMFCC.getStandardDeviation();
		
		for (int indexFrame = 0; indexFrame < mfccVector.length; indexFrame++) {
			System.out.print("MFCC: " + indexFrame + "	");
        	for (int indexCoefficient = 0; indexCoefficient < mfccVector[indexFrame].length; indexCoefficient++) {
        		System.out.print(mfccVector[indexFrame][indexCoefficient] + "	");
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
		
		return mfccVector;
    }
    
    /**
     * Exporta o espectrograma para um arquivo PNG.
     * 
     * @param imageToExport    - Imagem a ser exportada
     * @param strAudioFilePath - Caminho final do arquivo
     */
    public void exportSpectrogram(BufferedImage imageToExport, String strAudioFilePath) {
        try {
        	OutputStream outputStream = new FileOutputStream(new File(strAudioFilePath));
        	(new PngEncoder()).encode(imageToExport, outputStream);
        	outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Fecha o espectrograma.
     */
    public void closeSpectrogram() {
    	blnAllowGenerateTemporaryImages = false;
    }
}