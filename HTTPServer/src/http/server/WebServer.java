///Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Web Server TP
 *
 * Treat different HTTP requests. This version includes support for GET, POST, PUT, HEAD, DELETE requests
 * and offers support for multiple file formats, including ico, png, jpg, mp3, mp4, avi and others
 * This server is also capable of detecting different types of errors.
 *
 * @author Stefan Ristovski Aydin Akaydin
 * @version 1.0
 */
public class WebServer {

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
            System.err.println("Error: " + e);
            return;
        }

        System.out.println("Waiting for connection");
        for (;;) {
            BufferedInputStream in = null;
            BufferedOutputStream out = null;
            Socket remote = null;
            try {
                // wait for a connection
                remote = s.accept();
                // remote is now the connected socket
                System.out.println("Connection, opening IO stream.");
                in = new BufferedInputStream(remote.getInputStream());
                out = new BufferedOutputStream(remote.getOutputStream());

                // Read the header of incoming request
                System.out.println("Waiting for data...");
                String header = new String();


                // Header ends with: \r\n\r\n
                //parse characters of request until you get end of header
                int currentByte = '\0';
                int prevByte = '\0';
                boolean isNewLine = false;

                currentByte = in.read();
                while(currentByte != -1 && !(prevByte == '\r' && currentByte == '\n' && isNewLine)) {
                    if(prevByte == '\r' && currentByte == '\n') {
                        isNewLine = true;
                    } else if(!(prevByte == '\n' && currentByte == '\r')) {
                        isNewLine = false;
                    }
                    prevByte = currentByte;
                    header += (char) currentByte;
                    currentByte = in.read();
                }

                System.out.println("REQUEST :");
                System.out.println(header);

                // If currentByte == -1 There is an error with the header, it does not end by \r\n\r\n

                if(currentByte != -1 && !header.isEmpty()) {

                    String[] requestWords = header.split(" ");
                    String requestType = requestWords[0];
                    String resourceName = requestWords[1].substring(1, requestWords[1].length());

                    if (resourceName.isEmpty()) {
                        httpGET(out, "files/index.html");
                    } else if(requestType.equals("PUT")) {
                        httpPUT(in, out, resourceName);
                    } else if(requestType.equals("POST")) {
                        httpPOST(in, out, resourceName);
                    } else if(requestType.equals("HEAD")) {
                        httpHEAD(in, out, resourceName);
                    } else if(requestType.equals("DELETE")) {
                        httpDELETE(out, resourceName);
                    } else if(requestType.equals("GET")) {
                        if (resourceName.startsWith("files") || resourceName.startsWith("favicon")) {
                            httpGET(out, resourceName);
                        } else {
                            out.write(createHeader("403 Forbidden").getBytes());
                            out.flush();
                        }
                    } else {
                        out.write(createHeader("501 Not Implemented").getBytes());
                        out.flush();
                    }
                } else {
                    out.write(createHeader("400 Bad Request").getBytes());
                    out.flush();
                }
                remote.close();

            } catch (Exception e) {
                System.err.println("Error: " + e);
                try {
                    out.write(createHeader("500 Internal Server Error in START").getBytes());
                    out.flush();
                } catch (Exception e2) { System.err.println(e2); };
                try {
                    remote.close();
                } catch (Exception e2) { System.err.println(e2); }
            }
        }
    }


    /**
     * HTTP GET method.
     * Opens a certain resource and sends it to the client.
     * @param out to write to the client socket to send the resource to
     * @param fileName path of resource
     */
    protected void httpGET(BufferedOutputStream out, String fileName)
    {
        System.out.println("Called GET " + fileName);
        try {
            File resource = new File(fileName);
            if (resource.exists()) {
                out.write(createHeader("200 OK", fileName, resource.length()).getBytes());
            } else {
                resource = new File("files/notfound.html");
                out.write(createHeader("404 Not Found", "files/notfound.html", resource.length()).getBytes());
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
            System.err.println("Error: " + e);
            try {
                out.write(createHeader("500 Internal Server Error in HTTP GET").getBytes());
                out.flush();
            } catch (Exception e2) { System.err.println(e2); };
        }
    }

    /**
     * HTTP POST method
     * Add information to existing resource that is in the body of the request
     * @param in Read on the client socket
     * @param out Writing on the client socket for the response
     * @param fileName File path of the resource
     */
    protected void httpPOST(BufferedInputStream in, BufferedOutputStream out, String fileName) {
        System.out.println("Called POST " + fileName);
        try {
            File resource = new File(fileName);

            if(resource.exists())
            {
                out.write(createHeader("200 OK").getBytes());
            } else {
                out.write(createHeader("201 Created").getBytes());
                resource.createNewFile();
            }

            BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource, resource.exists()));

            byte[] buffer = new byte[256];
            while(in.available() > 0) {
                int nbRead = in.read(buffer);
                fileOut.write(buffer, 0, nbRead);
            }

            fileOut.flush();
            fileOut.close();
            out.flush();
        } catch (Exception e) {
            System.err.println("Error: " + e);
            try {
                out.write(createHeader("500 Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) { System.err.println(e2); };
        }
    }

    /**
     * HTTP HEAD method
     * Returns only the header of a GET request
     * @param in Reading on the client socket
     * @param out Writing on the client socket for the response
     * @param fileName Path of file
     */
    protected void httpHEAD(BufferedInputStream in, BufferedOutputStream out, String fileName) {
        System.out.println("Called HEAD " + fileName);
        try {
            File resource = new File(fileName);
            if(resource.exists() && resource.isFile()) {
                out.write(createHeader("200 OK", fileName, resource.length()).getBytes());
            } else {
                out.write(createHeader("404 Not Found").getBytes());
            }
            out.flush();
        } catch (IOException e) {
            System.err.println("Error: " + e);
            try {
                out.write(createHeader("500 Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) { System.err.println(e2); };
        }
    }

    /**
     * HTTP PUT method
     * Creates a resource with content given in the body of the request
     * @param in Reading on the client socket
     * @param out Writing on the client socket for the response
     * @param fileName Path of new File
     */
    protected void httpPUT(BufferedInputStream in, BufferedOutputStream out, String fileName) {
        System.out.println("Called PUT " + fileName);
        try {
            File resource = new File(fileName);

            if (resource.exists())
            {
                out.write(createHeader("204 No Content").getBytes());
            } else {
                out.write(createHeader("201 Created").getBytes());
                resource.createNewFile();
            }

            BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource));
            byte[] buffer = new byte[256];

            while(in.available() > 0) {
                int nbRead = in.read(buffer);
                fileOut.write(buffer, 0, nbRead);
            }

            fileOut.flush();
            fileOut.close();

            out.flush();
        } catch (Exception e) {
            System.err.println("Error: " + e);
            try {
                out.write(createHeader("500 Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) { System.err.println(e2); };
        }
    }

    /**
     * HTTP DELETE method
     * Deletes a resource
     * @param out Writing on the client socket for the response
     * @param fileName file path
     */
    protected void httpDELETE(BufferedOutputStream out, String fileName) {
        System.out.println("Called DELETE " + fileName);
        try {
            File resource = new File(fileName);
            boolean isDeleted = false;
            boolean doesExist = false;

            if (resource.exists())
            {
                if (resource.isFile())
                {
                    if( resource.delete() ) {
                        out.write(createHeader("204 No Content").getBytes());
                    }
                } else {
                    out.write(createHeader("403 Forbidden").getBytes());
                }
            } else {
                out.write(createHeader("404 Not Found").getBytes());
            }

            out.flush();
        } catch (Exception e) {
            System.err.println("Error: " + e);
            try {
                out.write(createHeader("500 Internal Server Error").getBytes());
                out.flush();
            } catch (Exception e2) { System.err.println(e2); };
        }
    }

    /**
     * Create a HTTP response without a body
     * @param status header code
     * @return Reponse
     */
    protected String createHeader(String status) {
        String header = "HTTP/1.0 " + status + "\r\nServer: Bot\r\n\r\n";
        System.out.println("Response header :");
        System.out.println(header);
        return header;
    }

    /**
     * Create a HTTP response with a body
     * @param status header code.
     * @param filename resource path included in the response body.
     * @param length Size in bytes of resource
     * @return Response
     */
    protected String createHeader(String status, String filename, long length) {
        String header = "HTTP/1.0 " + status + "\r\n";
        if(filename.endsWith(".html") || filename.endsWith(".htm"))
            header += "Content-Type: text/html\r\n";
        else if(filename.endsWith(".ico"))
            header += "Content-Type: image/x-icon\r\n";
        else if(filename.endsWith(".png"))
            header += "Content-Type: image/png\r\n";
        else if(filename.endsWith(".jpeg") || filename.endsWith(".jpg"))
            header += "Content-Type: image/jpg\r\n";
        else if(filename.endsWith(".mp3"))
            header += "Content-Type: audio/mp3\r\n";
        else if(filename.endsWith(".mp4"))
            header += "Content-Type: video/mp4\r\n";
        else if(filename.endsWith(".avi"))
            header += "Content-Type: video/x-msvideo\r\n";
        else if(filename.endsWith(".css"))
            header += "Content-Type: text/css\r\n";
        else if(filename.endsWith(".pdf"))
            header += "Content-Type: application/pdf\r\n";

        header += "Content-Length: " + length + "\r\nServer: Bot\r\n\r\n";

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
