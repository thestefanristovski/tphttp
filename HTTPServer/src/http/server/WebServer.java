///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 *
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 *
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

    protected static final String DIRECTORY = "files";
    /**Chemin relatif de la page web � envoyer en cas d'erreur 404*/
    protected static final String FILE_NOT_FOUND = "files/notfound.html";
    /**Chemin relatif de la page d'acceuil du serveur*/
    protected static final String INDEX = "files/index.html";

    /**
     * WebServer constructor.
     */
    protected void start() {
        ServerSocket s;

        System.out.println("Webserver starting up on port 3000");
        System.out.println("(press ctrl-c to exit)");
        try {
            // create the main server socket
            s = new ServerSocket(3000);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return;
        }

        System.out.println("Waiting for connection");
        for (;;) {
            BufferedReader in = null;
            BufferedOutputStream out = null;
            Socket remote = null;
            try {
                // wait for a connection
                remote = s.accept();
                // remote is now the connected socket
                System.out.println("Connection, opening IO stream.");
                in = new BufferedReader(new InputStreamReader(
                        remote.getInputStream()));
                out = new BufferedOutputStream(remote.getOutputStream());

                // read the data sent. We basically ignore it,
                // stop reading once a blank line is hit. This
                // blank line signals the end of the client HTTP
                // headers.

                // Read the header of incoming request
                System.out.println("Waiting for data...");
                String header = new String();


                // Header ends with: \r\n\r\n (CR LF CR LF)
                int bcur = '\0', bprec = '\0';
                boolean newline = false;
                while((bcur = in.read()) != -1 && !(newline && bprec == '\r' && bcur == '\n')) {
                    if(bprec == '\r' && bcur == '\n') {
                        newline = true;
                    } else if(!(bprec == '\n' && bcur == '\r')) {
                        newline = false;
                    }
                    bprec = bcur;
                    header += (char) bcur;
                }

                System.out.println("REQUEST :");
                System.out.println(header);

                // If bcur == -1 There is an error with the header, it does not end by \r\n\r\n

                if(bcur != -1 && !header.isEmpty()) {
                    String[] requestWords = header.split(" ");
                    String requestType = requestWords[0];
                    String resourceName = requestWords[1].substring(1, requestWords[1].length());
                    // Par d�faut, envoyer la page d'acceuil
                    if (resourceName.isEmpty()) {
                        httpGET(out, INDEX);
                    } else if (resourceName.startsWith(DIRECTORY) || resourceName.startsWith("favicon")) {
                        if(requestType.equals("GET")) {
                            httpGET(out, resourceName);
                        } else {
                            // Unknown request
                            out.write(makeHeader("501 Not Implemented").getBytes());
                            out.flush();
                        }
                    } else {
                        // Accessing files outside of DIRECTORY
                        out.write(makeHeader("403 Forbidden").getBytes());
                        out.flush();
                    }
                } else {
                    out.write(makeHeader("400 Bad Request").getBytes());
                    out.flush();
                }

                remote.close();
            } catch (Exception e) {
                System.out.println("Error: " + e);
                try {
                    out.write(makeHeader("500 Internal Server Error in START").getBytes());
                    out.flush();
                } catch (Exception e2) {};
                try {
                    remote.close();
                } catch (Exception e2) {}
            }
        }
    }

    protected void httpGET(BufferedOutputStream out, String fileName)
    {
        System.out.println("Called GET " + fileName);
        try {
            File resource = new File(fileName);
            if (resource.exists()) {
                out.write(makeHeader("200 OK", fileName, resource.length()).getBytes());
            } else {
                resource = new File(FILE_NOT_FOUND);
                out.write(makeHeader("404 Not Found ICON", FILE_NOT_FOUND, resource.length()).getBytes());
            }

            BufferedInputStream fileRead = new BufferedInputStream(new FileInputStream(resource));
            byte[] buffer = new byte[256];
            int nbRead;
            while((nbRead = fileRead.read(buffer)) != -1) {
                out.write(buffer, 0, nbRead);
            }
            fileRead.close();
            out.flush();
        } catch (IOException e) {
            System.out.println("Error: " + e);
            try {
                out.write(makeHeader("500 Internal Server Error in HTTP GET").getBytes());
                out.flush();
            } catch (Exception e2) {};
        }
    }

    protected String makeHeader(String status) {
        String header = "HTTP/1.0 " + status + "\r\n" + "Server: Bot\r\n" + "\r\n";
        System.out.println("Response header :");
        System.out.println(header);
        return header;
    }

    protected String makeHeader(String status, String filename, long length) {
        String header = "HTTP/1.0 " + status + "\r\n";
        header += "Content-Type: text/html\r\n";
        header += "Content-Length: " + length + "\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println("Response header :");
        System.out.println(header);
        return header;
    }




    /**
     * Start the application.
     *
     * @param args
     *            Command line parameters are not used.
     */
    public static void main(String args[]) {
        WebServer ws = new WebServer();
        ws.start();
    }
}
