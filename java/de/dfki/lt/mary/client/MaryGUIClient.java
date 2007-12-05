/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.client;

//General Java Classes
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;

import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.JavaStreamingAudioPlayer;

import de.dfki.lt.mary.client.MaryClient.Voice;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.effects.AudioEffectsSet;
import de.dfki.lt.signalproc.effects.BaseAudioEffect;
import de.dfki.lt.signalproc.effects.EffectsApplier;



/**
 * A GUI Interface to the Mary Client, allowing to access and modify
 * intermediate processing results.
 * @author Marc Schr&ouml;der
 * @see MaryClient The client implementation
 */

public class MaryGUIClient extends JPanel
{

    /* -------------------- GUI stuff -------------------- */

    private Dimension paneDimension;
    // Input
    private JPanel inputTypePanel;
    private JComboBox cbInputType;
    private JPanel inputPanel;
    private JScrollPane inputScrollPane;
    private JTextPane inputText;
    private JPanel voicePanel;
    private JComboBox cbDefaultVoice;
    private JComboBox cbVoiceExampleText;
    private boolean doReplaceInput = true;
    // When the user changes input type, he is offered an example text for
    // the new input type. In order to prevent this when setting a new input
    // type from within the program, doReplaceInput must be set to false
    // before triggering the selection changed event.

    // Output
    private boolean showingTextOutput = true;
    private JPanel outputTypePanel;
    private JComboBox cbOutputType;
    private JButton bSaveOutput;
    private JTextPane outputText;
    private JScrollPane outputScrollPane;
    private JPanel audioPanel;
    private JButton bPlay;
    private JPanel savePanel;

    //Audio effects
    private boolean isButtonHide = true;
    private boolean showingAudioEffects = false;
    private JPanel showHidePanel;
    private JButton showHideEffects;
    private AudioEffectsSet audioEffectSet;
    private JList effectsList;
    private MaryAudioEffectsBox effectsBox;
    //

    // Processing Buttons
    private JPanel buttonPanel;
    private JButton bProcess;
    private JButton bEdit;
    private JButton bCompare;

    static JFrame mainFrame;
    static JApplet mainApplet;

    /* -------------------- Data and Processing stuff -------------------- */
    private MaryClient processor;

    private AudioPlayer audioPlayer = null;
    private Vector availableVoices = null;
    private Vector inputTypes = null;
    private Vector outputTypes = null;
    private boolean allowSave;
    private boolean streamMp3 = false;
    private MaryClient.Voice prevVoice = null;

    //Map of limited Domain Voices and their example Texts
    private Map limDomVoices = new HashMap();
    
    //Map of voices and their audio effects
    private Map voices = new HashMap();

    static FocusTraversalPolicy maryGUITraversal;

    /**
     * Create a MaryGUIClient instance that connects to the server host
     * and port as specified in the system properties "server.host" and "server.port",
     * which default to "cling.dfki.uni-sb.de" and 59125, respectively.
     * @throws IOException
     * @throws UnknownHostException
     */
    public MaryGUIClient() throws IOException, UnknownHostException
    {
        super();
        // First the MaryClient processor class, because it may provide
        // information needed in the GUI creation.
        try {
            processor = new MaryClient();
            streamMp3 = Boolean.getBoolean("stream.mp3");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    e.getMessage(),
                    "Cannot connect to server",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        allowSave = true;
        init();
    }

    /**
     * Create a MaryGUIClient instance that connects to the given server host
     * and port. This is meant to be used from Applets.
     * @param host
     * @param port
     * @throws IOException
     * @throws UnknownHostException
     */
    public MaryGUIClient(String host, int port, JApplet applet) throws IOException, UnknownHostException
    {
        super();
        // First the MaryClient processor class, because it may provide
        // information needed in the GUI creation.
        try {
            processor = new MaryClient(host, port, false, false);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    e.getMessage(),
                    "Cannot connect to server",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        mainApplet = applet;
        allowSave = false;
        init();
    }

    /**
     * Create an instance of the MaryClient class which does the processing,
     * and initialise the GUI.
     */
    public void init() throws IOException, UnknownHostException {

        maryGUITraversal = new MaryGUIFocusTraversalPolicy();
        //if this is a normal gui
        if (mainFrame != null){
            mainFrame.setFocusTraversalPolicy(maryGUITraversal);
        } else { //this is an applet
            mainApplet.setFocusTraversalPolicy(maryGUITraversal);
        }

        audioEffectSet = null;
        
        paneDimension = new Dimension(250,400);
        // Layout
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridC = new GridBagConstraints();

        gridC.insets = new Insets( 2,2,2,2 );
        gridC.weightx = 0.1;
        gridC.weighty = 0.1;
        setLayout( gridBagLayout );

        //////////////// Left Column: Input /////////////////////
        // Input type
        inputTypePanel = new JPanel();
        inputTypePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        gridC.gridx = 0;
        gridC.gridy = 0;
        gridC.gridwidth = 3;
        gridC.fill = GridBagConstraints.HORIZONTAL;
        gridBagLayout.setConstraints( inputTypePanel, gridC );
        add( inputTypePanel );
        gridC.gridwidth = 1;
        JLabel inputTypeLabel = new JLabel( "Input Type: " );
        inputTypePanel.add(inputTypeLabel);
        inputTypes = processor.getInputDataTypes();
        outputTypes = processor.getOutputDataTypes();
        assert inputTypes.size() > 0;
        assert outputTypes.size() > 0;
        cbInputType = new JComboBox( inputTypes );
        cbInputType.setName("Input Type");
        cbInputType.getAccessibleContext().setAccessibleName("Input Type selection");
        cbInputType.setToolTipText( "Specify the type of data contained " +
        "in the input text area below." );
        cbInputType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    verifyDefaultVoices();
                    verifyExamplesVisible();
                    if (doReplaceInput) {
                        setExampleInputText();
                    } else {
                        // input text was set by other code
                        doReplaceInput = true;
                    }
                    setOutputTypeItems();
                }
            }
        });
        inputTypePanel.add( cbInputType );

        //Input Text area
        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setMinimumSize(paneDimension);
        inputPanel.setPreferredSize(paneDimension);
        gridC.gridx = 0;
        gridC.gridy = 1;
        gridC.gridwidth = 3;
        gridC.gridheight = 3;
        gridC.weightx = 0.4;
        gridC.weighty = 0.8;
        //gridC.ipadx = 270;
        //gridC.ipady = 200;
        gridC.fill = GridBagConstraints.BOTH;
        gridBagLayout.setConstraints( inputPanel, gridC );
        add( inputPanel );
        gridC.gridwidth = 1;
        gridC.gridheight = 1;
        gridC.weightx = 0.1;
        gridC.weighty = 0.1;
        gridC.ipadx = 0;
        gridC.ipady = 0;
        gridC.fill = GridBagConstraints.NONE;
        inputText = new JTextPane();

        inputText.getAccessibleContext().setAccessibleName("Input Text Area");

        //Set Tab and Shift-Tab for Keyboard movement
        Set forwardKeys = new HashSet();
        forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false));
        inputText.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,forwardKeys);
        Set backwardKeys = new HashSet();
        backwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,KeyEvent.SHIFT_MASK+KeyEvent.SHIFT_DOWN_MASK, false));
        inputText.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,backwardKeys);

        inputScrollPane = new JScrollPane(inputText);
        inputPanel.add(inputScrollPane);
        inputScrollPane.setPreferredSize(new Dimension(inputPanel.getPreferredSize().width, 1000));
        //example text for limDom voices
        cbVoiceExampleText = new JComboBox();
        cbVoiceExampleText.setName("Example Text");
        cbVoiceExampleText.getAccessibleContext().setAccessibleName("Example text selection");
        cbVoiceExampleText.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (doReplaceInput
                            && ((MaryClient.DataType)cbInputType.getSelectedItem()).name().startsWith("TEXT"))
                        setInputText((String)cbVoiceExampleText.getSelectedItem());
                }
            }
        });
        cbVoiceExampleText.setPreferredSize(new Dimension(inputPanel.getPreferredSize().width, 25));
        inputPanel.add(cbVoiceExampleText);

        // Select voice
        voicePanel = new JPanel();
        voicePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        gridC.gridx = 0;
        gridC.gridy = 4;
        gridC.gridwidth = 4;
        gridC.gridheight = 2;
        gridC.fill = GridBagConstraints.HORIZONTAL;
        gridBagLayout.setConstraints( voicePanel, gridC );
        add( voicePanel );
        gridC.gridwidth = 1;
        gridC.gridheight = 1;
        JLabel voiceLabel = new JLabel("Voice:");
        voicePanel.add( voiceLabel );
        cbDefaultVoice = new JComboBox();
        cbDefaultVoice.setName("Voice selection");
        cbDefaultVoice.getAccessibleContext().setAccessibleName("Voice selection");
        voicePanel.add( cbDefaultVoice );
        cbDefaultVoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillExampleTexts();
                    verifyExamplesVisible();
                    MaryClient.Voice voice = (MaryClient.Voice)cbDefaultVoice.getSelectedItem();
                    MaryClient.DataType dataType = (MaryClient.DataType)cbInputType.getSelectedItem(); 
                    if (doReplaceInput
                            && (voice.isLimitedDomain() && dataType.name().startsWith("TEXT")
                                    || getPrevVoice() == null
                                    || !getPrevVoice().getLocale().equals(voice.getLocale())))
                        setExampleInputText();
                    setPrevVoice(voice);
                    setAudioEffects(voice);
                }
            }
        });
        // For the limited domain voices, get example texts: 
        availableVoices = processor.getVoices();
        Iterator it = availableVoices.iterator();
        while (it.hasNext()) {
            MaryClient.Voice v = (MaryClient.Voice) it.next();
            if (v.isLimitedDomain()){
                String exampleText = processor.getVoiceExampleText(v.name());
                limDomVoices.put(v.name(), processVoiceExampleText(exampleText));
            }
        }
        verifyDefaultVoices();
        fillExampleTexts();
        verifyExamplesVisible();
        setExampleInputText();

        //////////////// Centre Column: Buttons /////////////////////
        // Action buttons in centre
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        gridC.gridx = 3;
        gridC.gridy = 1;
        gridC.gridheight = 3;
        gridC.fill = GridBagConstraints.BOTH;
        gridBagLayout.setConstraints( buttonPanel, gridC );
        add( buttonPanel );
        bProcess = new JButton( "Process ->" );
        bProcess.setToolTipText( "Call the Mary Server." +
        "The input will be transformed into the specified output type." );
        bProcess.getAccessibleContext().setAccessibleName("Process button");
        bProcess.setActionCommand( "process" );
        bProcess.setMnemonic('P');
        bProcess.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                processInput();
                verifyEnableButtons();
            }
        });
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add( bProcess );
        bProcess.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(Box.createVerticalGlue());

        bEdit = new JButton( "<- Edit" );
        bEdit.setToolTipText( "Edit the content of the output text area as the new input." +
        " The current content of the input text area will be discarded." );
        bEdit.getAccessibleContext().setAccessibleName("Edit button");
        bEdit.setActionCommand( "edit" );
        bEdit.setMnemonic('E');
        bEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editOutput();
                verifyEnableButtons();
            }
        });
        buttonPanel.add( bEdit );
        bEdit.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(Box.createVerticalGlue());

        bCompare = new JButton( "<- Compare ->" );
        bCompare.setToolTipText( "Compare input and output" +
            "(available only if both are MaryXML types)." );
        bCompare.getAccessibleContext().setAccessibleName("Compare button");
        bCompare.setActionCommand( "compare" );
        bCompare.setMnemonic('C');
        bCompare.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compareTexts();
                verifyEnableButtons();
            }
        });
        buttonPanel.add( bCompare );
        bCompare.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.setPreferredSize(new Dimension(buttonPanel.getPreferredSize().width, paneDimension.height));

        //////////////// Right Column: Output /////////////////////
        // Output type
        outputTypePanel = new JPanel();
        outputTypePanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        gridC.gridx = 4;
        gridC.gridy = 0;
        gridC.gridwidth = 3;
        gridC.gridheight = 1;
        gridC.ipady = 10;
        gridC.fill = GridBagConstraints.HORIZONTAL;
        gridBagLayout.setConstraints( outputTypePanel, gridC );
        add( outputTypePanel );
        gridC.ipady = 0;
        gridC.gridwidth = 1;
        JLabel outputTypeLabel = new JLabel( "Output Type: " );
        outputTypePanel.add( outputTypeLabel );
        cbOutputType = new JComboBox();
        cbOutputType.setName("Output type");
        cbOutputType.getAccessibleContext().setAccessibleName("Output type selection");
        setOutputTypeItems();
        // The last possible output type (= audio) is the default
        // output type:
        cbOutputType.setSelectedIndex(cbOutputType.getItemCount() - 1);
        cbOutputType.setToolTipText( "Specify the output type for the next " +
        "processing action (Process button)." );
        cbOutputType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    verifyOutputDisplay();
                    verifyEnableButtons();
                    revalidate();
                }
            }
        });
        outputTypePanel.add( cbOutputType );

        // Output Text area
        if (((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType())
            showingTextOutput = true;
        else
            showingTextOutput = false;
        outputText = new JTextPane();
        outputText.getAccessibleContext().setAccessibleName("Output text");

        //set tab and shift-tab for keyboard movement
        outputText.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,forwardKeys);
        outputText.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,backwardKeys);

        //        outputText.setLineWrap(false);
        outputText.setEditable(false);
        outputScrollPane = new JScrollPane(outputText);
        outputScrollPane.setMinimumSize(paneDimension);
        outputScrollPane.setPreferredSize(paneDimension);
        gridC.gridx = 4;
        gridC.gridy = 1;
        gridC.gridwidth = 3;
        gridC.gridheight = 3;
        gridC.weightx = 0.4;
        gridC.weighty = 0.8;
        //gridC.ipadx = 270;
        //gridC.ipady = 200;
        gridC.fill = GridBagConstraints.BOTH;
        gridBagLayout.setConstraints( outputScrollPane, gridC );
        if (!showingTextOutput)
            outputScrollPane.setVisible(false);
        add( outputScrollPane );
       
        //Audio effects
        MaryClient.Voice voice = (MaryClient.Voice)cbDefaultVoice.getSelectedItem();
        setAudioEffects(voice);
        
        isButtonHide = false;
        if (audioEffectSet!=null)
        {
            showAudioEffects(gridBagLayout, gridC);

            if (effectsBox!=null && effectsBox.mainPanel!=null)
            {
                showHidePanel = new JPanel();
                showHidePanel.setPreferredSize(paneDimension);
                showHidePanel.setLayout( new BoxLayout(showHidePanel, BoxLayout.Y_AXIS) );
                if (!showingTextOutput)
                {
                    showHideEffects = new JButton("Hide Effects");
                    isButtonHide = true;
                }
                else
                {
                    showHideEffects = new JButton("Show Effects");
                    isButtonHide = false;
                }

                showHideEffects.setToolTipText( "Hide or show available audio effects for post-processing the TTS output" );
                showHideEffects.getAccessibleContext().setAccessibleName("Hide/Show audio effects button");

                showHideEffects.addActionListener( new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showHideEffectAction();
                    }
                });
            
                showHidePanel.add(Box.createVerticalGlue());
                showHidePanel.add(showHideEffects);
                showHidePanel.add(Box.createVerticalGlue());
                showHideEffects.setAlignmentX(Component.CENTER_ALIGNMENT);

                gridC.gridx = 4;
                if (!showingTextOutput)
                    gridC.gridy = 4;
                else
                    gridC.gridy = 3;
                    
                gridC.gridwidth = 3;
                gridC.gridheight = 1;
                gridC.ipady = 10;
                gridC.fill = GridBagConstraints.BOTH;
                gridBagLayout.setConstraints(showHidePanel, gridC);
                add(showHidePanel);
                gridC.ipady = 0;
                
                if (effectsBox.mainPanel!=null && !showingTextOutput && isButtonHide)
                {
                    effectsBox.mainPanel.setVisible(true);
                    showingAudioEffects = true;
                }
                else
                {
                    effectsBox.mainPanel.setVisible(false);
                    showingAudioEffects = false;
                }
            }
        }   
        //
        
        gridC.gridwidth = 1;
        gridC.gridheight = 1;
        gridC.weightx = 0.1;
        gridC.weighty = 0.1;
        gridC.ipadx = 0;
        gridC.ipady = 0;
        gridC.fill = GridBagConstraints.NONE;
        
        // Overlapping location: Audio play button
        audioPanel = new JPanel();
        audioPanel.setPreferredSize(paneDimension);
        audioPanel.setLayout( new BoxLayout(audioPanel, BoxLayout.Y_AXIS) );
        bPlay = new JButton( "Play" );
        bPlay.setToolTipText( "Synthesize and play the resulting audio stream." );
        bPlay.getAccessibleContext().setAccessibleName("Play button");
        bPlay.setActionCommand( "play" );
        bPlay.setMnemonic('P');
        bPlay.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (audioPlayer != null) { // an audioPlayer is currently playing
                    audioPlayer.cancel();
                    audioPlayer = null;
                    bPlay.setText("Play");
                } else {
                    processInput();
                }
            }
        });
        audioPanel.add(Box.createVerticalGlue());
        audioPanel.add(bPlay);
        audioPanel.add(Box.createVerticalGlue());
        bPlay.setAlignmentX(Component.CENTER_ALIGNMENT);
        //bPlay.setMaximumSize(bPlay.getPreferredSize());

        if (showingTextOutput)
            audioPanel.setVisible(false);
        gridC.gridx = 4;
        if (showingAudioEffects)
            gridC.gridy = 5;
        else
            gridC.gridy = 4;
        gridC.gridwidth = 3;
        gridC.gridheight = 1;
        gridC.ipady = 10;
        gridC.fill = GridBagConstraints.BOTH;
        gridBagLayout.setConstraints(audioPanel, gridC);
        add( audioPanel );
        gridC.gridwidth = 1;
        gridC.ipady = 0;

        // Output Save button        
        if (allowSave) {
            savePanel = new JPanel();
            savePanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
            gridC.gridx = 4;
            gridC.ipady = 10;
            if (showingAudioEffects)
                gridC.gridy = 6;
            else
                gridC.gridy = 5;
            
            gridC.fill = GridBagConstraints.HORIZONTAL;
            gridBagLayout.setConstraints( savePanel, gridC );
            add(savePanel);
            ImageIcon saveIcon = new ImageIcon("save.gif");
            bSaveOutput = new JButton( "Save...", saveIcon );
            bSaveOutput.setToolTipText( "Save the output as a file." );
            bSaveOutput.getAccessibleContext().setAccessibleName("Save Output button");
            bSaveOutput.setActionCommand( "saveOutput" );
            bSaveOutput.setMnemonic('S');
            bSaveOutput.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    saveOutput();
                }
            });
            savePanel.add( bSaveOutput );
            gridC.ipady = 0;
        }
        setPreferredSize(new Dimension(720,480));

        verifyEnableButtons();
        cbInputType.requestFocusInWindow();
    }
    
    private void setAudioEffects(Voice voice)
    {
        String availableAudioEffects = "";
        try {
            availableAudioEffects = processor.getAudioEffects(voice.name());
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        String strAudioEffects = "";
        String effectClassName; 
        StringTokenizer st = new StringTokenizer(availableAudioEffects, "\n");

        boolean bFirst = true;
        while (st.hasMoreTokens())
        {
            effectClassName = (String)st.nextToken();
            effectClassName = effectClassName.trim();
            BaseAudioEffect ae = null;
            try {
                ae = (BaseAudioEffect)Class.forName(effectClassName).newInstance();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            if (ae!=null)
            {   
                if (!bFirst)
                    strAudioEffects += EffectsApplier.chEffectSeparator + ae.getFullEffectWithExampleParametersAsString();
                else
                {
                    strAudioEffects += ae.getFullEffectWithExampleParametersAsString();
                    bFirst=false;
                }
            }     
        }

        audioEffectSet = new AudioEffectsSet(strAudioEffects);
    }
    
    private void showHideEffectAction()
    {
        if (isButtonHide)
        {
            if (effectsBox!=null && effectsBox.mainPanel!=null)
                effectsBox.mainPanel.setVisible(false);

            showHideEffects.setText("Show Effects");
            isButtonHide = false;
            showingAudioEffects = false;
        }
        else
        {
            if (effectsBox!=null && effectsBox.mainPanel!=null)
                effectsBox.mainPanel.setVisible(true);

            showHideEffects.setText("Hide Effects");
            isButtonHide = true;
            showingAudioEffects = true;
        }
    }
    
    //If there are any effects available for the selected voice
    // update audio effects box accordingly
    private void showAudioEffects(GridBagLayout g, GridBagConstraints c)
    {
        //Overlapping location: Audio effects box
        //Initialize the effects here (normally using info from the server)
        if (audioEffectSet!=null && audioEffectSet.totalEffects>0)
        {
            effectsBox = new MaryAudioEffectsBox(audioEffectSet.getEffectNames(), audioEffectSet.getExampleParams(), audioEffectSet.getHelpTexts());
            if (effectsBox != null)
            {  
                c.gridx = 4;
                c.gridy = 1;
                c.gridwidth = 3;
                c.gridheight = 3;
                c.weightx = 0.1;
                c.weighty = 0.1;
                c.ipadx = 0;
                c.ipady = 0;
                add(effectsBox.mainPanel, c);
                effectsBox.show();
            }
        }
        //
    }

    //Create a single String parameter by reading the selected ffect parameters in the interface
    //If no effect is selected or the effects are not being shown, an empty String is returned
    private String getAudioEffectsAsString()
    {
        
        String strParams = "";
        String strTmpParam;

        if (effectsBox!=null)
        {
            if (isButtonHide)
            {         
                boolean bFirst = true;
                for (int i=0; i<audioEffectSet.totalEffects; i++)
                {
                    if (effectsBox.effectControls[i].chkEnabled.isSelected())
                    {
                        audioEffectSet.effects[i].setParams(effectsBox.effectControls[i].txtParams.getText());

                        effectsBox.effectControls[i].txtParams.setText(audioEffectSet.effects[i].getParamsAsString(false));

                        strTmpParam = audioEffectSet.effects[i].getFullEffectAsString();

                        if (!bFirst)
                            strParams += EffectsApplier.chEffectSeparator + strTmpParam;
                        else
                        {
                            strParams += strTmpParam;
                            bFirst = false;
                        }
                    }
                }
            }
        }
        return strParams;
    }
    
    private void setExampleInputText()
    {

        MaryClient.Voice defaultVoice = (MaryClient.Voice) cbDefaultVoice.getSelectedItem();
        MaryClient.DataType inputType = (MaryClient.DataType) cbInputType.getSelectedItem();
        if (defaultVoice.isLimitedDomain() && inputType.name().startsWith("TEXT")) {
            setInputText((String) cbVoiceExampleText.getSelectedItem());
        } else {
            try {
                String key = inputType.name();
                String exampleText;
                if (inputType.getLocale() == null) {
                    // for data types without locale, test if we can get example text.
                    // If not, try to get example text with voice locale.
                    try {
                        exampleText = processor.getServerExampleText(key);
                    } catch (IOException err) {
                        key = inputType.name() + "_" + defaultVoice.getLocale().getLanguage().toUpperCase();
                        exampleText = processor.getServerExampleText(key);
                    }
                } else {
                    exampleText = processor.getServerExampleText(key);
                }
                setInputText(exampleText);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fillExampleTexts()
    {
        MaryClient.Voice defaultVoice = (MaryClient.Voice) cbDefaultVoice.getSelectedItem();
        if (!defaultVoice.isLimitedDomain()) return;
        Vector sentences = (Vector)limDomVoices.get(defaultVoice.name());
        assert sentences != null;
        cbVoiceExampleText.removeAllItems();
        for (int i = 0; i<sentences.size(); i++) {
            cbVoiceExampleText.addItem(sentences.get(i));
        }
        cbVoiceExampleText.setSelectedIndex(0);
    }

    private void verifyExamplesVisible()
    {
        MaryClient.Voice defaultVoice = (MaryClient.Voice)cbDefaultVoice.getSelectedItem();
        MaryClient.DataType inputType = (MaryClient.DataType) cbInputType.getSelectedItem();

        if (defaultVoice.isLimitedDomain() && inputType.name().startsWith("TEXT")) {
            cbVoiceExampleText.setVisible(true);
        } else {
            cbVoiceExampleText.setVisible(false);
        }
    }

    private void verifyEnableButtons() {
        if (((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType()) {
            buttonPanel.setVisible(true);
            if (!cbOutputType.hasFocus()) bProcess.requestFocusInWindow();
        } else { // do not show these three buttons for audio output:
            buttonPanel.setVisible(false);
            if (!cbOutputType.hasFocus()) bPlay.requestFocusInWindow();
        }
        // Edit button:
        if (showingTextOutput) {
            if (outputText.getText().length() == 0) {
                if (allowSave) bSaveOutput.setEnabled(false);
                bEdit.setEnabled(false);
            } else {
                if (allowSave) bSaveOutput.setEnabled(true);
                bEdit.setEnabled(true);
            }
        } else { // audio output
            if (allowSave) bSaveOutput.setEnabled(true);
        }
        // Compare button:
        // Only enabled if both input and output are text types
        if (((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType() &&
                outputText.getText().length() > 0) {
            bCompare.setEnabled(true);
        } else {
            bCompare.setEnabled(false);
        }
    }

    /**
     * Verify that the list of voices in cbDefaultVoices matches the language of the input format.
     */
    private void verifyDefaultVoices() 
    {
        MaryClient.DataType inputType = (MaryClient.DataType)cbInputType.getSelectedItem();
        Locale inputLocale = null;
        if (inputType != null) inputLocale = inputType.getLocale();
        // Is the default voice still suitable for the input locale?
        MaryClient.Voice defaultVoice = (MaryClient.Voice)cbDefaultVoice.getSelectedItem();
        Locale voiceLocale = null;
        if (defaultVoice != null) voiceLocale = defaultVoice.getLocale();
        if (inputLocale != null && voiceLocale != null && !voiceLocale.equals(inputLocale))
            defaultVoice = null;
        // Reset the list, just in case
        cbDefaultVoice.removeAllItems();
        Iterator it = availableVoices.iterator();
        while (it.hasNext()) {
            MaryClient.Voice v = (MaryClient.Voice) it.next();
            if (inputLocale == null || v.getLocale().equals(inputLocale)) {
                cbDefaultVoice.addItem(v);
            }
        }
        if (defaultVoice != null) {
            cbDefaultVoice.setSelectedItem(defaultVoice);
        } else { // First in list is default voice:
            cbDefaultVoice.setSelectedIndex(0);
        }
    }

    /**
     * Divides the example text of a voice into
     * sentences in a vector
     * @param text the example text
     * @return vector of example sentences
     */
    private Vector processVoiceExampleText(String text){
        StringTokenizer st = new StringTokenizer(text,"#");
        Vector sentences = new Vector();
        while (st.hasMoreTokens()){
            sentences.add(st.nextToken());}
        return sentences;
    }

    private void setOutputTypeItems()
    {
        MaryClient.DataType inputType = (MaryClient.DataType) cbInputType.getSelectedItem();
        Locale inputLocale = inputType.getLocale();
        MaryClient.DataType selectedItem = (MaryClient.DataType) cbOutputType.getSelectedItem();
        cbOutputType.removeAllItems();
        for (Iterator it = outputTypes.iterator(); it.hasNext(); ) {
            MaryClient.DataType d = (MaryClient.DataType) it.next();
            Locale locale = d.getLocale();
            if (inputLocale == null ||
                    locale == null ||
                    inputLocale.equals(locale)) {
                cbOutputType.addItem(d);
            }
        }
        cbOutputType.setSelectedItem(selectedItem);
    }

    private void verifyOutputDisplay()
    {
        if (((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType()) 
        {
            setOutputText(""); // erase the output text
            if (!showingTextOutput) 
            { 
                // showing Audio Output
                // need to change output display
                audioPanel.setVisible(false);

                if (effectsBox!=null)
                {
                    if (effectsBox.mainPanel!=null)
                        effectsBox.mainPanel.setVisible(false);
                
                    showHidePanel.setVisible(false);
                }
                
                outputScrollPane.setVisible(true);
                showingTextOutput = true;
                revalidate();
            }
        } 
        else 
        { // Audio output
            if (showingTextOutput) 
            {
                // change output display
                outputScrollPane.setVisible(false);
                audioPanel.setVisible(true);
                
                if (effectsBox!=null && effectsBox.mainPanel!=null)
                 {
                    showHidePanel.setVisible(true);
                    
                    if (effectsBox.mainPanel!=null && isButtonHide)
                        effectsBox.mainPanel.setVisible(true);
                 }
                
                showingTextOutput = false;
                revalidate();
            }
        }
    }


    /* -------------------- Processing callers -------------------- */
    private File lastDirectory = null;
    private String lastExtension = null;
    private void saveOutput()
    {
        if (!allowSave) return;
        try {
            if (showingTextOutput) {
                JFileChooser fc = new JFileChooser();
                if (lastDirectory != null) {
                    fc.setCurrentDirectory(lastDirectory);
                }
                int returnVal = fc.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fc.getSelectedFile();
                    lastDirectory = saveFile.getParentFile();
                    PrintWriter w = new PrintWriter(new FileWriter(saveFile));
                    w.print(outputText.getText());
                    w.close();
                }
            } else { // audio data
                JFileChooser fc = new JFileChooser();
                if (lastDirectory != null) {
                    fc.setCurrentDirectory(lastDirectory);
                }
                String[] knownAudioTypes = processor.getAudioFileFormatTypes();
                String[] extensions = new String[knownAudioTypes.length];
                String[] typeNames = new String[knownAudioTypes.length];
                FileFilter defaultFilter = null;
                for (int i=0; i<knownAudioTypes.length; i++) {
                    int iSpace = knownAudioTypes[i].indexOf(' ');
                    extensions[i] = knownAudioTypes[i].substring(0, iSpace);
                    typeNames[i] = knownAudioTypes[i].substring(iSpace+1);
                    FileFilter ff = new SimpleFileFilter(extensions[i],
                            typeNames[i] + " (." + extensions[i] + ")");
                    fc.addChoosableFileFilter(ff);
                    if (lastExtension != null && lastExtension.equals(extensions[i])) {
                        defaultFilter = ff;
                    }
                    if (defaultFilter != null) {
                        fc.setFileFilter(defaultFilter);
                    }
                }
                int returnVal = fc.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fc.getSelectedFile();
                    String ext = MaryUtils.getExtension(saveFile);
                    if (ext == null) { // no extension in the file name, append from filefilter
                        ext = ((SimpleFileFilter)fc.getFileFilter()).getExtension();
                        saveFile = new File(saveFile.getAbsolutePath()+"."+ext);
                    }
                    lastDirectory = saveFile.getParentFile();
                    lastExtension = ext;
                    String audioType = null;
                    for (int i=0; i<knownAudioTypes.length; i++) {
                        if (extensions[i].equals(ext)) {
                            audioType = typeNames[i];
                            break;
                        }
                    }
                    if (audioType == null) { // file has unknown extension
                        showErrorMessage("Unknown audio type",
                                "Cannot write file of type `." + ext + "'");
                    } else { // OK, we know what to do
                        processor.process(inputText.getText(),
                                ((MaryClient.DataType)cbInputType.getSelectedItem()).name(),
                                "AUDIO",
                                audioType,
                                ((MaryClient.Voice)cbDefaultVoice.getSelectedItem()).name(),
                                "",
                                getAudioEffectsAsString(),
                                new FileOutputStream(saveFile));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("IOException",e.getMessage());
        }
    }

    private void makeTextPlain(StyledDocument doc) {
        SimpleAttributeSet emptyAttributes = new SimpleAttributeSet();
        doc.setCharacterAttributes(0, doc.getLength(), emptyAttributes, true);
    }

    /**
     * Set everything that is not between < and > to bold.
     * This is "dumb", i.e. it will not try to analyse the contents of
     * tags, and fail at situations like <tag attr="3>i<4">, where i would
     * be printed in bold as well.
     */
    private void highlightText(StyledDocument doc) {
        SimpleAttributeSet highlighted = new SimpleAttributeSet();
        StyleConstants.setBold(highlighted, true);
        boolean insideTag = false;
        int beginText = -1; // will contain beginning of text to be highlighted
        for (int i=0; i<doc.getLength(); i++) {
            char c = ' '; // Initialisation to keep compiler happy
            try { c = doc.getText(i,1).charAt(0); }
            catch (BadLocationException e) {}
            if (insideTag) {
                if (c == '>')
                    insideTag = false;
            } else { // not inside a tag
                if (c == '<') {
                    // Start of new tag
                    if (beginText != -1) { // anything to highlight?
                        // highlight it
                        doc.setCharacterAttributes(beginText, i-beginText,
                                highlighted, false);
                        beginText = -1;
                    }
                    insideTag = true;
                } else { // normal text character
                    if (beginText == -1) {
                        // This is the first text character
                        beginText = i;
                    }
                }
            }
        } // for all characters in document
        // Any text at the very end of the document?
        if (beginText != -1) {
            doc.setCharacterAttributes(beginText, doc.getLength()-beginText,
                    highlighted, false);
        }
    }

    // Call the mary client
    private void processInput()
    {
        OutputStream os;
        MaryClient.DataType outputType = (MaryClient.DataType)cbOutputType.getSelectedItem();
        if (outputType.name().equals("AUDIO")) {
            try {
                audioPlayer = new JavaStreamingAudioPlayer();
                processor.streamAudio(inputText.getText(), 
                        ((MaryClient.DataType)cbInputType.getSelectedItem()).name(),
                        streamMp3 ? "MP3":"WAVE",
                                ((MaryClient.Voice)cbDefaultVoice.getSelectedItem()).name(),
                                "",
                                getAudioEffectsAsString(),
                                audioPlayer,
                                new MaryClient.AudioPlayerListener() {
                    public void playerFinished()
                    {
                        resetPlayButton();
                    }
                    public void playerException(Exception e)
                    {
                        showErrorMessage(e.getClass().getName(), e.getMessage());
                        resetPlayButton();
                    }
                });
                bPlay.setText("Stop");
            } catch (Exception e) {
                e.printStackTrace();
                showErrorMessage(e.getClass().getName(), e.getMessage());
                resetPlayButton();
            }

        } else {
            try {
                // Write to a byte array (to be converted to a string later)
                os = new ByteArrayOutputStream();
                processor.process(inputText.getText(),
                        ((MaryClient.DataType)cbInputType.getSelectedItem()).name(),
                        outputType.name(),
                        null,
                        ((MaryClient.Voice)cbDefaultVoice.getSelectedItem()).name(),
                        "",
                        getAudioEffectsAsString(),
                        os);

                try {
                    setOutputText(((ByteArrayOutputStream)os).toString("UTF-8"));
                } catch (UnsupportedEncodingException uee) {
                    uee.printStackTrace();
                }
                bEdit.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
                showErrorMessage(e.getClass().getName(), e.getMessage());
            }
        }
    }

    private void editOutput() {
        MaryClient.DataType type = (MaryClient.DataType) cbOutputType.getSelectedItem();
        if (type == null || !type.isTextType() || !type.isInputType())
            return;
        setInputText(outputText.getText());
        setOutputText("");
        // We need to make sure the item handler doesn't try to replace
        // the input with a default example:
        if (cbInputType.getSelectedItem().equals(cbOutputType.getSelectedItem())) {
            // No problem, type won't change anyway
        } else {
            // Signal to the item handler that we don't want replacement
            doReplaceInput = false;
            cbInputType.setSelectedItem(cbOutputType.getSelectedItem());
        }
    }

    private void compareTexts() {
        // Only try to compare if both are MaryXML and non-empty:
        if (!((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType() ||
                inputText.getText().length() == 0 ||
                outputText.getText().length() == 0) {
            return;
        }
        try {
            // First, make both documents plain text:
            makeTextPlain(inputText.getStyledDocument());
            makeTextPlain(outputText.getStyledDocument());

            // Now, highlight text in both documents:
            highlightText(inputText.getStyledDocument());
            highlightText(outputText.getStyledDocument());

            // Define text attributes for added/removed chunks:
            SimpleAttributeSet removed = new SimpleAttributeSet();
            SimpleAttributeSet added = new SimpleAttributeSet();
            StyleConstants.setBold(removed, true);
            StyleConstants.setBold(added, true);
            StyleConstants.setItalic(removed, true);
            StyleConstants.setItalic(added, true);
            StyleConstants.setUnderline(added, true);
            StyleConstants.setForeground(removed, Color.red);
            StyleConstants.setForeground(added, Color.green.darker());
            // Calculate the differences between input and output:
            String input = inputText.getStyledDocument().getText(0, inputText.getStyledDocument().getLength());
            String[] inputWords = MaryUtils.splitIntoSensibleXMLUnits(input);
            int[] inputIndex = new int[inputWords.length+1];
            int total = 0;
            for (int i=0; i<inputWords.length; i++) {
                inputIndex[i] = total;
                total += inputWords[i].length();
                //System.err.println("Input Word nr. " + i + ": [" + inputWords[i] + "], indexes " + inputIndex[i] + "-" + (inputIndex[i]+inputWords[i].length()) + "[" + input.substring(inputIndex[i], inputIndex[i]+inputWords[i].length()) + "] / [" + inputText.getStyledDocument().getText(inputIndex[i], inputWords[i].length()) + "]");
            }
            inputIndex[inputWords.length] = total;
            String output = outputText.getStyledDocument().getText(0, outputText.getStyledDocument().getLength());
            String[] outputWords = MaryUtils.splitIntoSensibleXMLUnits(output);
            int[] outputIndex = new int[outputWords.length+1];
            total = 0;
            for (int i=0; i<outputWords.length; i++) {
                outputIndex[i] = total;
                total += outputWords[i].length();
                //System.err.println("Output Word nr. " + i + ": [" + outputWords[i] + "], indexes " + outputIndex[i] + "-" + (outputIndex[i]+outputWords[i].length()) + "[" + output.substring(outputIndex[i], outputIndex[i]+outputWords[i].length()) + "]");
            }
            outputIndex[outputWords.length] = total;
            List diffs = new Diff(inputWords, outputWords).diff();
            Iterator it = diffs.iterator();
            while (it.hasNext()) {
                Difference diff = (Difference)it.next();
                int delStart = diff.getDeletedStart();
                int delEnd = diff.getDeletedEnd();
                int addStart = diff.getAddedStart();
                int addEnd = diff.getAddedEnd();
                if (delEnd != Difference.NONE) {
                    inputText.getStyledDocument().setCharacterAttributes(inputIndex[delStart], inputIndex[delEnd+1]-inputIndex[delStart], removed, false);
                    //System.err.println("deleted "+delStart+"-"+(delEnd+1)+": [" + input.substring(inputIndex[delStart], inputIndex[delEnd+1]) + "] / [" + inputText.getStyledDocument().getText(inputIndex[delStart], inputIndex[delEnd+1]-inputIndex[delStart]) + "]");
                }
                if (addEnd != Difference.NONE) {
                    outputText.getStyledDocument().setCharacterAttributes(outputIndex[addStart], outputIndex[addEnd+1]-outputIndex[addStart], added, false);                
                    //System.err.println("added "+addStart+"-"+(addEnd+1)+": [" + output.substring(outputIndex[addStart], outputIndex[addEnd+1]) + "] / [" + outputText.getStyledDocument().getText(outputIndex[addStart], outputIndex[addEnd+1]-outputIndex[addStart]) + "]");
                }
            }
        } catch(Exception ex) { ex.printStackTrace(); }
    }

    protected void setInputText(String text)
    {
        inputText.setText(text);
        makeTextPlain(inputText.getStyledDocument());
        inputText.setCaretPosition(0);
    }

    protected void setOutputText(String text)
    {
        outputText.setText(text);
        makeTextPlain(outputText.getStyledDocument());
        outputText.setCaretPosition(0);
    }

    private MaryClient.Voice getPrevVoice() { return prevVoice; }
    private void setPrevVoice(MaryClient.Voice prevVoice) {
        this.prevVoice = prevVoice;
    }

    public void resetPlayButton()
    {
        bPlay.setText("Play");
        if (audioPlayer != null) {
            audioPlayer.cancel();
            audioPlayer = null;
        }
    }

    protected void showErrorMessage(String title, String message)
    {
        JOptionPane.showMessageDialog(this,
                message +
                "\n\nIf you think this is a bug in the MARY system,\n" +
                "please help improve the system by filing a bug report\n" +
                "on the MARY development page: \n" +
                "http://mary.opendfki.de/newticket\n",
                title,
                JOptionPane.ERROR_MESSAGE);

    }

    public static void main(String[] args) throws Exception 
    {
        mainFrame = new JFrame("Mary GUI Client");
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        MaryGUIClient m = new MaryGUIClient();
        mainFrame.setContentPane(m);
        mainFrame.pack();
        mainFrame.setVisible(true);

    }

    class MaryGUIFocusTraversalPolicy
    extends FocusTraversalPolicy {

        public Component getComponentAfter(Container focusCycleRoot,
                Component aComponent) 
        {
            if (aComponent.equals(cbInputType)) {
                return cbOutputType;
            } else if (aComponent.equals(cbOutputType)) {
                return cbDefaultVoice;
            } else if (aComponent.equals(cbDefaultVoice)) {
                if (cbVoiceExampleText.isVisible()){
                    return cbVoiceExampleText;
                } else {
                    return inputText;
                }
            } else if (aComponent.equals(cbVoiceExampleText)) {
                return inputText;
            } else if (aComponent.equals(inputText)) {
                if (audioPanel.isVisible()){
                    return bPlay;
                } else {
                    return bProcess;
                }
            } else if (aComponent.equals(bProcess)) {
                if (bEdit.isEnabled()){
                    return bEdit;
                } else {
                    if (allowSave && bSaveOutput.isEnabled()){
                        return bSaveOutput;
                    } else {
                        return cbInputType;
                    }
                }
            } else if (aComponent.equals(bPlay)) {
                if (allowSave){
                    return bSaveOutput;
                } else {
                    return cbInputType;
                }
            } else if (aComponent.equals(outputText)) {
                if (bEdit.isEnabled()){
                    return bEdit;
                } else {
                    return cbInputType;
                }
            } else if (aComponent.equals(bEdit)) {
                return bCompare;
            } else if (aComponent.equals(bCompare)) {
                if (allowSave){
                    return bSaveOutput;
                } else {
                    return cbInputType;
                }
            } else if (aComponent.equals(bSaveOutput)) {
                return cbInputType;
            }
            return cbInputType;
        }

        public Component getComponentBefore(Container focusCycleRoot,
                Component aComponent) 
        {
            if (aComponent.equals(bSaveOutput)) {
                if (!buttonPanel.isVisible()){

                    return bPlay;
                } else {
                    if (bCompare.isEnabled()){

                        return bCompare;
                    } else {

                        return bProcess;
                    }
                }
            } else if (aComponent.equals(bCompare)) {
                return bEdit;
            } else if (aComponent.equals(bEdit)) {
                return bProcess;
            } else if (aComponent.equals(outputText)) {
                return bProcess;
            } else if (aComponent.equals(bPlay)) {
                return inputText;
            } else if (aComponent.equals(bProcess)) {
                return inputText;
            } else if (aComponent.equals(inputText)) {
                if (cbVoiceExampleText.isVisible()){
                    return cbVoiceExampleText;
                } else {
                    return cbDefaultVoice;
                }
            } else if (aComponent.equals(cbVoiceExampleText)) {
                return cbDefaultVoice;
            } else if (aComponent.equals(cbDefaultVoice)) {
                return cbOutputType;
            } else if (aComponent.equals(cbOutputType)) {
                return cbInputType;
            } else if (aComponent.equals(cbInputType)) {
                if (allowSave  && bSaveOutput.isEnabled()){
                    return bSaveOutput;
                } else {
                    if (buttonPanel.isVisible()){
                        return bProcess;
                    } else {
                        return bPlay;
                    }
                }
            }
            return cbInputType;
        }

        public Component getDefaultComponent(Container focusCycleRoot) 
        {
            return cbInputType;
        }

        public Component getLastComponent(Container focusCycleRoot) 
        {
            return bSaveOutput;
        }

        public Component getFirstComponent(Container focusCycleRoot) 
        {
            return cbInputType;
        }
    }

}
