//
// OMELoginPane.java
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

package loci.visbio.ome;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import javax.swing.*;
import loci.visbio.util.DialogPane;

/**
 * OMELoginPane provides a dialog for obtaining
 * OME login information from the user.
 */
public class OMELoginPane extends DialogPane {

  // -- GUI components --

  /** Text field for OME server. */
  protected JTextField serv;

  /** Text field for OME username. */
  protected JTextField user;

  /** Text field for OME password. */
  protected JPasswordField pass;


  // -- Constructor --

  /** Constructs an OME login dialog. */
  public OMELoginPane() {
    super("Login to OME");

    // server URL
    serv = new JTextField(20);
    serv.setToolTipText("The OME server address for data upload");

    // username
    user = new JTextField(8);
    user.setToolTipText("The OME username under which to upload");

    // password
    pass = new JPasswordField(8);
    pass.setToolTipText("Password for the specified OME username");

    // lay out components
    PanelBuilder builder = new PanelBuilder(new FormLayout(
      "right:pref, 3dlu, pref:grow, 5dlu, pref, 3dlu, pref:grow",
      "pref, 3dlu, pref"));
    CellConstraints cc = new CellConstraints();

    builder.addLabel("&Server", cc.xy(1, 1)).setLabelFor(serv);
    builder.add(serv, cc.xyw(3, 1, 5));

    builder.addLabel("&Username", cc.xy(1, 3)).setLabelFor(user);
    builder.add(user, cc.xy(3, 3));
    builder.addLabel("&Password", cc.xy(5, 3)).setLabelFor(pass);
    builder.add(pass, cc.xy(7, 3));

    add(builder.getPanel());
  }


  // -- OMELoginPane API methods --

  /** Gets the server specified in the login pane. */
  public String getServer() {
    String server = serv.getText();
    if (server.startsWith("http:")) {
      server = server.substring(5);
    }
    while (server.startsWith("/")) server = server.substring(1);
    int slash = server.indexOf("/");
    if (slash >= 0) server = server.substring(0, slash);
    int colon = server.indexOf(":");
    if (colon >= 0) server = server.substring(0, colon);

    return "http://" + server + "/shoola/";
  }

  /** Gets the user name specified in the login pane. */
  public String getUser() { return user.getText(); }

  /** Gets the password specified in the login pane. */
  public String getPassword() { return new String(pass.getPassword()); }

}