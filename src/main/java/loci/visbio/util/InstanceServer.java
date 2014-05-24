/*
 * #%L
 * VisBio application for visualization of multidimensional biological
 * image data.
 * %%
 * Copyright (C) 2002 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.visbio.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * The InstanceServer provides a mechanism for managing the instances of an
 * application running concurrently. The application should attempt to send any
 * desired parameters to an already active instance server using the static
 * sendArguments method. If successful (no exception thrown), the application
 * should then shut down, having successfully passed its arguments to the active
 * application instance. If unsuccessful (caught an exception), the application
 * should create an InstanceServer, thus becoming the active application
 * instance.
 */
public class InstanceServer implements Runnable {

	// -- Fields --

	/** Server socket listening for client connections. */
	protected ServerSocket serverSocket;

	/** List of application instance spawn listeners. */
	protected Vector listeners;

	/** Whether this instance server is still listening for spawn events. */
	protected boolean alive = true;

	// -- Static InstanceServer API methods --

	/**
	 * Attempts to send the given arguments to an instance server running on the
	 * specified port. Returns true if successful, indicating the application
	 * should shut down since an instance is already running and has received the
	 * parameters. Returns false if unsuccessful, indicating the application
	 * should create its own InstanceServer and become the active application
	 * instance.
	 */
	public static void sendArguments(final String[] args, final int port)
		throws IOException
	{
		final Socket socket = new Socket("localhost", port);
		final PrintWriter out =
			new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		out.println(args == null ? -1 : args.length);
		for (int i = 0; i < args.length; i++)
			out.println(args[i]);
		out.close();
		socket.close();
	}

	// -- Constructor --

	/** Creates a new instance server on the given port. */
	public InstanceServer(final int port) throws IOException {
		serverSocket = new ServerSocket(port, 1);
		listeners = new Vector();
		new Thread(this, "InstanceServer").start();
	}

	// -- InstanceServer API methods --

	/** Adds an application instance spawn listener. */
	public void addSpawnListener(final SpawnListener l) {
		synchronized (listeners) {
			listeners.addElement(l);
		}
	}

	/** Removes an application instance spawn listener. */
	public void removeSpawnListener(final SpawnListener l) {
		synchronized (listeners) {
			listeners.removeElement(l);
		}
	}

	/** Removes all application instance spawn listeners. */
	public void removeAllListeners() {
		synchronized (listeners) {
			listeners.removeAllElements();
		}
	}

	/** Stops this instance server's thread. */
	public void stop() {
		alive = false;
		try {
			serverSocket.close();
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
	}

	// -- Runnable API methods --

	/**
	 * Listens for connections from newly spawned application instances and passes
	 * their arguments to all registered listeners.
	 */
	@Override
	public void run() {
		while (alive) {
			try {
				final Socket socket = serverSocket.accept();
				final BufferedReader in =
					new BufferedReader(new InputStreamReader(socket.getInputStream()));
				int numArgs = -1;
				try {
					final String line = in.readLine();
					if (line != null) numArgs = Integer.parseInt(line);
				}
				catch (final NumberFormatException exc) {}
				final String[] args = numArgs < 0 ? null : new String[numArgs];
				for (int i = 0; i < numArgs; i++)
					args[i] = in.readLine();
				in.close();
				socket.close();
				notifyListeners(new SpawnEvent(args));
			}
			catch (final IOException exc) {
				if (alive) exc.printStackTrace();
			}
		}
	}

	// -- Helper methods --

	/**
	 * Notifies application instance spawn listeners of a newly spawned
	 * application instance.
	 */
	protected void notifyListeners(final SpawnEvent e) {
		synchronized (listeners) {
			for (int i = 0; i < listeners.size(); i++) {
				final SpawnListener l = (SpawnListener) listeners.elementAt(i);
				l.instanceSpawned(e);
			}
		}
	}

}
