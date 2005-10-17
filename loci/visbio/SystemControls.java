//
// SystemControls.java
//

/*
VisBio application for visualization of multidimensional
biological image data. Copyright (C) 2002-2005 Curtis Rueden.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.visbio;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.*;
import loci.visbio.matlab.MatlabUtil;
import loci.visbio.util.*;
import visad.VisADException;
import visad.data.qt.QTForm;
import visad.util.ReflectedUniverse;

/** SystemControls is the control panel for reporting system information. */
public class SystemControls extends ControlPanel implements ActionListener {

  // -- GUI components --

  /** Memory usage text field. */
  private JTextField memField;


  // -- Fields --

  /** Current memory usage. */
  protected String memUsage;


  // -- Constructor --

  /** Constructs a control panel for viewing system information. */
  public SystemControls(LogicManager logic) {
    super(logic, "System", "Reports system information");
    VisBioFrame bio = lm.getVisBio();
    SystemManager sm = (SystemManager) lm;

    // dump properties button
    JButton dump = new JButton("Dump all");
    if (!LAFUtil.isMacLookAndFeel()) dump.setMnemonic('d');
    dump.setToolTipText("Dumps system property values to the output console");
    dump.setActionCommand("dump");
    dump.addActionListener(this);

    // operating system text field
    JTextField osField = new JTextField(System.getProperty("os.name") +
      " (" + System.getProperty("os.arch") + ")");
    osField.setEditable(false);

    // java version text field
    JTextField javaField = new JTextField(System.getProperty("java.version") +
      " (" + System.getProperty("java.vendor") + ")");
    javaField.setEditable(false);

    // memory usage text field
    memField = new JTextField("xxxx MB used (xxxx MB reserved)");
    memField.setEditable(false);

    // garbage collection button
    JButton clean = new JButton("Clean");
    if (!LAFUtil.isMacLookAndFeel()) clean.setMnemonic('c');
    clean.setToolTipText(
      "Calls the Java garbage collector to free wasted memory");
    clean.setActionCommand("clean");
    clean.addActionListener(this);

    // memory maximum text field
    JTextField heapField = new JTextField(
      sm.getMaximumMemory() + " MB maximum");
    heapField.setEditable(false);

    // memory maximum alteration button
    JButton heap = new JButton("Change...");
    if (!LAFUtil.isMacLookAndFeel()) heap.setMnemonic('a');
    if (sm.isJNLP()) heap.setEnabled(false);
    heap.setToolTipText(
      "Edits the maximum amount of memory available to VisBio");
    heap.setActionCommand("heap");
    heap.addActionListener(this);

    // Java3D library text field
    JTextField java3dField = new JTextField(
      getVersionString("javax.vecmath.Point3d"));
    java3dField.setEditable(false);

    // JPEG library text field
    JTextField jpegField = new JTextField(
      getVersionString("com.sun.image.codec.jpeg.JPEGCodec"));
    jpegField.setEditable(false);

    // QuickTime library text field
    String qtVersion = null;
    try {
      System.out.println("Constructing QT form");
      QTForm qtForm = new QTForm();
      if (qtForm.isQTExpired()) qtVersion = "Expired";
      else {
        ReflectedUniverse r = qtForm.getUniverse();
        String qtMajor = r.exec("QTSession.getMajorVersion()").toString();
        String qtMinor = r.exec("QTSession.getMinorVersion()").toString();
        qtVersion = qtMajor + "." + qtMinor;
      }
    }
    catch (VisADException exc) { qtVersion = "Missing"; }
    JTextField qtField = new JTextField(qtVersion);
    qtField.setEditable(false);

    // Python library text field
    JTextField pythonField = new JTextField(
      getVersionString("org.python.util.PythonInterpreter"));
    pythonField.setEditable(false);

    // MATLAB library text field
    String matlabVersion = MatlabUtil.getMatlabVersion();
    JTextField matlabField = new JTextField(
      matlabVersion == null ? "Missing" : matlabVersion);
    matlabField.setEditable(false);

    // JAI library text field
    JTextField jaiField = new JTextField(
      getVersionString("javax.media.jai.JAI"));
    jaiField.setEditable(false);

    // Look & Feel text field
    JTextField lafField = new JTextField(LAFUtil.getLookAndFeel()[0]);
    lafField.setEditable(false);

    // Look & Feel alteration button
    JButton laf = new JButton("Change...");
    if (!LAFUtil.isMacLookAndFeel()) laf.setMnemonic('n');
    if (sm.isJNLP()) laf.setEnabled(false);
    laf.setToolTipText("Edits VisBio's graphical Look & Feel");
    laf.setActionCommand("laf");
    laf.addActionListener(this);

    // Stereo configuration text field
    JTextField stereoField = new JTextField(
      DisplayUtil.getStereoConfiguration() == null ?
      "Not available" : "Enabled");
    stereoField.setEditable(false);

    // lay out components
    FormLayout layout = new FormLayout(
      "right:pref, 3dlu, pref:grow, 3dlu, pref",
      "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 9dlu, " +
      "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, " +
      "pref, 3dlu, pref, 9dlu, pref, 3dlu, pref, 3dlu, pref");
    PanelBuilder builder = new PanelBuilder(layout);
    builder.setDefaultDialogBorder();
    CellConstraints cc = new CellConstraints();
    int row = 1;

    builder.addSeparator("Properties", cc.xyw(1, row, 3));
    builder.add(dump, cc.xy(5, row));
    row += 2;
    builder.addLabel("&Operating system", cc.xy(1, row)).setLabelFor(osField);
    builder.add(osField, cc.xyw(3, row, 3));
    row += 2;
    builder.addLabel("&Java version", cc.xy(1, row)).setLabelFor(javaField);
    builder.add(javaField, cc.xyw(3, row, 3));
    row += 2;
    builder.addLabel("Memory &usage", cc.xy(1, row)).setLabelFor(memField);
    builder.add(memField, cc.xy(3, row));
    builder.add(clean, cc.xy(5, row));
    row += 2;
    builder.addLabel("Memory ma&ximum", cc.xy(1, row)).setLabelFor(heapField);
    builder.add(heapField, cc.xy(3, row));
    builder.add(heap, cc.xy(5, row));
    row += 2;
    builder.addSeparator("Libraries", cc.xyw(1, row, 5));
    row += 2;
    builder.addLabel("Java&3D", cc.xy(1, row)).setLabelFor(java3dField);
    builder.add(java3dField, cc.xyw(3, row, 3));
    row += 2;
    builder.addLabel("JPE&G", cc.xy(1, row)).setLabelFor(jpegField);
    builder.add(jpegField, cc.xyw(3, row, 3));
    row += 2;
    builder.addLabel("&QuickTime", cc.xy(1, row)).setLabelFor(qtField);
    builder.add(qtField, cc.xyw(3, row, 3));
    row += 2;
    builder.addLabel("&Python", cc.xy(1, row)).setLabelFor(pythonField);
    builder.add(pythonField, cc.xyw(3, row, 3));
    row += 2;
    builder.addLabel("&MATLAB", cc.xy(1, row)).setLabelFor(matlabField);
    builder.add(matlabField, cc.xyw(3, row, 3));
    row += 2;
    builder.addLabel("JA&I", cc.xy(1, row)).setLabelFor(jaiField);
    builder.add(jaiField, cc.xyw(3, row, 3));
    row += 2;
    builder.addSeparator("Configuration", cc.xyw(1, row, 5));
    row += 2;
    builder.addLabel("&Look && Feel", cc.xy(1, row)).setLabelFor(lafField);
    builder.add(lafField, cc.xy(3, row));
    builder.add(laf, cc.xy(5, row));
    row += 2;
    builder.addLabel("&Stereo", cc.xy(1, row)).setLabelFor(stereoField);
    builder.add(stereoField, cc.xyw(3, row, 3));
    row += 2;
    add(builder.getPanel());

    // update system information twice per second
    Timer t = new Timer(500, this);
    t.start();
  }


  // -- ActionListener API methods --

  /** Handles action events. */
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    SystemManager sm = (SystemManager) lm;
    if ("dump".equals(cmd)) {
      Properties properties = System.getProperties();
      // Properties.list() truncates the property values, so we iterate
      //properties.list(System.out);
      System.out.println("-- listing properties --");
      Enumeration list = properties.propertyNames();
      while (list.hasMoreElements()) {
        String key = (String) list.nextElement();
        String value = properties.getProperty(key);
        System.out.println(key + "=" + value);
      }
    }
    else if ("clean".equals(cmd)) sm.cleanMemory();
    else if ("heap".equals(cmd)) {
      String max = "" + sm.getMaximumMemory();
      String heapSize = (String) JOptionPane.showInputDialog(this,
        "New maximum memory value:", "VisBio", JOptionPane.QUESTION_MESSAGE,
        null, null, "" + max);
      if (heapSize == null || heapSize.equals(max)) return;
      int maxHeap = -1;
      try { maxHeap = Integer.parseInt(heapSize); }
      catch (NumberFormatException exc) { }
      if (maxHeap < 16) {
        JOptionPane.showMessageDialog(this,
          "Maximum memory value must be at least 16 MB.",
          "VisBio", JOptionPane.ERROR_MESSAGE);
        return;
      }
      sm.writeScript(maxHeap, null);
      JOptionPane.showMessageDialog(this,
        "The change will take effect next time you run VisBio.",
        "VisBio", JOptionPane.INFORMATION_MESSAGE);
    }
    else if ("laf".equals(cmd)) {
      String[] laf = LAFUtil.getLookAndFeel();
      final String[][] lafs = LAFUtil.getAvailableLookAndFeels();
      String lafName = (String) JOptionPane.showInputDialog(this,
        "New Look & Feel:", "VisBio", JOptionPane.QUESTION_MESSAGE,
        null, lafs[0], laf[0]);
      if (lafName == null) return;
      int ndx = -1;
      for (int i=0; i<lafs[0].length; i++) {
        if (lafs[0][i].equals(lafName)) {
          ndx = i;
          break;
        }
      }
      if (ndx < 0 || lafs[1][ndx].equals(laf[1])) return;
      sm.writeScript(-1, lafs[1][ndx]);
      JOptionPane.showMessageDialog(this,
        "The change will take effect next time you run VisBio.",
        "VisBio", JOptionPane.INFORMATION_MESSAGE);
    }
    else {
      // update system information
      if (!lm.getVisBio().isVisible()) return;
      String mem = ((SystemManager) lm).getMemoryUsage();
      if (!mem.equals(memUsage)) {
        memUsage = mem;
        memField.setText(mem);
      }
    }
  }


  // -- Helper methods --

  /** Gets version information for the specified class. */
  private String getVersionString(String clas) {
    Class c = null;
    try { c = Class.forName(clas); }
    catch (ClassNotFoundException exc) { c = null; }
    return getVersionString(c);
  }

  /** Gets version information for the specified class. */
  private String getVersionString(Class c) {
    if (c == null) return "Missing";
    Package p = c.getPackage();
    if (p == null) return "No package";
    String vendor = p.getImplementationVendor();
    String version = p.getImplementationVersion();
    if (vendor == null && version == null) return "Installed";
    else if (vendor == null) return version;
    else if (version == null) return vendor;
    else return version + " (" + vendor + ")";
  }

}
