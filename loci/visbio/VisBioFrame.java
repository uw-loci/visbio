//
// VisBioFrame.java
//

/*
VisBio application for visualization of multidimensional
biological image data. Copyright (C) 2002-2004 Curtis Rueden.

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

import java.awt.BorderLayout;
import java.awt.Container;

import java.awt.event.ActionEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;

import loci.visbio.data.DataManager;

import loci.visbio.help.HelpManager;

//import loci.visbio.measure.AnnManager;

import loci.visbio.ome.OMEManager;

import loci.visbio.state.OptionManager;
import loci.visbio.state.StateManager;

import loci.visbio.util.LAFUtil;
import loci.visbio.util.SplashScreen;

import loci.visbio.view.DisplayManager;

import visad.util.GUIFrame;
import visad.util.ReflectedUniverse;
import visad.util.Util;

/** VisBioFrame is the main GUI frame for VisBio. */
public class VisBioFrame extends GUIFrame {

  // -- Constants --

  /** Debugging flag for event logic. */
  public static final boolean DEBUG = false;

  /** Flag indicating operating system is Mac OS X. */
  public static final boolean MAC_OS_X =
    System.getProperty("os.name").indexOf("Mac OS X") >= 0;


  // -- Fields --

  /** Logic managers. */
  protected Vector managers;

  /** Associated splash screen. */
  protected SplashScreen ss;

  /** Status bar. */
  protected JProgressBar status;

  /** Stop button for various operations. */
  protected JButton stop;


  // -- Constructor --

  /** Constructs a new VisBio frame with no splash screen. */
  public VisBioFrame() { this(null); }

  /** Constructs a new VisBio frame with the associated splash screen. */
  public VisBioFrame(SplashScreen ss) {
    super(true);
    setTitle(VisBio.TITLE);
    managers = new Vector();
    this.ss = ss;

    // initialize Look & Feel parameters
    LAFUtil.initLookAndFeel();

    // make menus appear in the right order
    getMenu("File");
    getMenu("Edit");
    getMenu("Window");
    getMenu("Help");

    // create status bar
    status = new JProgressBar();
    status.setStringPainted(true);
    status.setToolTipText("Reports status of VisBio operations");
    status.setBorder(new BevelBorder(BevelBorder.RAISED));

    // create stop button
    stop = new JButton("Stop");
    if (!LAFUtil.isMacLookAndFeel()) stop.setMnemonic('s');
    stop.setToolTipText("Stops the currently reported VisBio operation");
    stop.setBorder(new CompoundBorder(
      new BevelBorder(BevelBorder.RAISED), stop.getBorder()));
    stop.setPreferredSize(stop.getPreferredSize()); // force constant size

    // add status bar and stop button
    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BorderLayout());
    statusPanel.add(status, BorderLayout.CENTER);
    statusPanel.add(stop, BorderLayout.EAST);
    Container c = getContentPane();
    c.setLayout(new BorderLayout());
    c.add(statusPanel, BorderLayout.SOUTH);
    resetStatus();

    // create logic managers
    OptionManager om = new OptionManager(this);
    StateManager sm = new StateManager(this);
    WindowManager wm = new WindowManager(this);
    sm.setRestoring(true);
    LogicManager[] lm = {
      sm, // StateManager
      om, // OptionManager
      wm, // WindowManager
      new PanelManager(this),
      new HelpManager(this),
      new DataManager(this),
      new OMEManager(this),
      new DisplayManager(this),
      //new AnnManager(this),
      new SystemManager(this),
      new ConsoleManager(this),
      new ExitManager(this)
    };
    int tasks = 1;
    for (int i=0; i<lm.length; i++) tasks += lm[i].getTasks();
    if (ss != null) ss.setTasks(tasks);

    // add logic managers
    for (int i=0; i<lm.length; i++) addManager(lm[i]);
    setSplashStatus("Finishing");

    // read configuration file
    om.readIni();

    // handle Mac OS X application menu items
    if (MAC_OS_X) {
      ReflectedUniverse r = new ReflectedUniverse();
      r.setDebug(true);
      try {
        r.exec("import loci.visbio.MacAdapter");
        r.setVar("bio", this);
        r.exec("MacAdapter.link(bio)");
      }
      catch (Exception exc) { exc.printStackTrace(); }
    }

    // distribute menu bar across all frames
    if (LAFUtil.isMacLookAndFeel()) wm.setDistributedMenus(true);

    // show VisBio window onscreen
    pack();
    Util.centerWindow(this);
    show();

    // hide splash screen
    if (ss != null) {
      ss.hide();
      ss = null;
    }

    // determine if VisBio crashed last time
    sm.setRestoring(false);
    sm.checkCrash();
  }


  // -- VisBioFrame API methods --

  /** Updates the splash screen to report the given status message. */
  public void setSplashStatus(String s) {
    if (ss == null) return;
    if (s != null) ss.setText(s + "...");
    ss.nextTask();
  }

  /** Adds a logic manager to the VisBio interface. */
  public void addManager(LogicManager lm) {
    managers.add(lm);
    generateEvent(new VisBioEvent(lm, VisBioEvent.LOGIC_ADDED, null, false));
  }

  /** Gets the logic manager of the given class. */
  public LogicManager getManager(Class c) {
    for (int i=0; i<managers.size(); i++) {
      LogicManager lm = (LogicManager) managers.elementAt(i);
      if (lm.getClass().equals(c)) return lm;
    }
    return null;
  }

  /** Gets all logic managers. */
  public LogicManager[] getManagers() {
    LogicManager[] lm = new LogicManager[managers.size()];
    managers.copyInto(lm);
    return lm;
  }

  /** Generates a state change event. */
  public void generateEvent(Object src, String msg, boolean undo) {
    generateEvent(new VisBioEvent(src, VisBioEvent.STATE_CHANGED, msg, undo));
  }

  /** Generates an event and notifies all linked logic managers. */
  public void generateEvent(VisBioEvent evt) {
    if (DEBUG) System.out.println(evt.toString());
    for (int i=0; i<managers.size(); i++) {
      LogicManager lm = (LogicManager) managers.elementAt(i);
      lm.doEvent(evt);
    }
  }

  /**
   * This method executes action commands of the form
   *
   *   "loci.visbio.data.DataManager.importData(param)"
   *
   * by stripping off the fully qualified class name, checking VisBio's
   * list of logic managers for one of that class, and calling the given
   * method on that object, optionally with the given String parameter.
   */
  public void call(String cmd) {
    try {
      // determine class from fully qualified name
      int dot = cmd.lastIndexOf(".");
      Class c = Class.forName(cmd.substring(0, dot));

      // determine if action command has an argument
      Class[] param = null;
      String[] s = null;
      int paren = cmd.indexOf("(");
      if (paren >= 0) {
        param = new Class[] {String.class};
        s = new String[] {cmd.substring(paren + 1, cmd.indexOf(")", paren))};
      }
      else paren = cmd.length();

      // execute appropriate method of that logic manager
      LogicManager lm = getManager(c);
      if (lm != null) {
        Method method = c.getMethod(cmd.substring(dot + 1, paren), param);
        if (method != null) method.invoke(lm, s);
      }
    }
    catch (ClassNotFoundException exc) { exc.printStackTrace(); }
    catch (IllegalAccessException exc) { exc.printStackTrace(); }
    catch (IllegalArgumentException exc) { exc.printStackTrace(); }
    catch (InvocationTargetException exc) {
      Throwable t = exc.getTargetException();
      if (t == null) exc.printStackTrace();
      else t.printStackTrace();
    }
    catch (NoSuchMethodException exc) { exc.printStackTrace(); }
  }

  /** Gets the status bar's progress bar. */
  public JProgressBar getProgressBar() { return status; }

  /** Gets the status bar's stop button. */
  public JButton getStopButton() { return stop; }

  /** Resets status bar to an idle state. */
  public void resetStatus() {
    status.setString(VisBio.TITLE + " " + VisBio.VERSION);
    status.setStringPainted(true);
    status.setIndeterminate(false);
    status.setValue(0);
    stop.setEnabled(false);
  }


  // -- ActionListener API methods --

  /**
   * Internal method enabling logic managers to access
   * their own methods via VisBio's menu system.
   */
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.lastIndexOf(".") < 0) super.actionPerformed(e);
    else call(cmd);
  }

}
