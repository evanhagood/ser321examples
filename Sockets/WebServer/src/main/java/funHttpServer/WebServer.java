/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;
import org.json.*;

class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * 
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * 
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(
                "Would theoretically be a file but removed this part, you do not have to do anything with it for the assignment");
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          query_pairs = splitQuery(request.replace("multiply?", ""));

          // extract required fields from parameters
          Integer result;
          try {
            Integer num1 = Integer.parseInt(query_pairs.get("num1"));
            Integer num2 = Integer.parseInt(query_pairs.get("num2"));

            // do math
            result = num1 * num2;

            // Generate response
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Result is: " + result);
            // this method will still overflow if the passed integers multiply to something
            // > Integer.MAX_VALUE
            // I'm not gonna change the code to use something like BigInteger, though that's
            // probably what I'd do if this were a real server
          } catch (NumberFormatException ex) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Please ensure passed parameters are valid integers.");
          } catch (StringIndexOutOfBoundsException ex) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Please enter two parameters for multiply.");
          } catch (Exception ex) {
            // This could be executed by a client error, but since I'm catching Exception,
            // I'll respond with 500 since this could also be a server error. Client can
            // make sure their usage is correct.
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("An unexpected error occured. Usage: /multiply?num1=<num1>&num2=<num2>");
          }

          // TODO: Include error handling here with a correct error code and
          // a response that makes sense

        } else if (request.contains("github?")) {
          // pulls the query from the request and runs it with GitHub's REST API
          // check out https://docs.github.com/rest/reference/
          //
          // HINT: REST is organized by nesting topics. Figure out the biggest one first,
          // then drill down to what you care about
          // "Owner's repo is named RepoName. Example: find RepoName's contributors"
          // translates to
          // "/repos/OWNERNAME/REPONAME/contributors"

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          query_pairs = splitQuery(request.replace("github?", ""));
          String json = null;
          try {
            json = fetchURL("https://api.github.com/" + query_pairs.get("query"));
            // System.out.println(json); // keep commented out - for testing: possibility
            // json is null here
          } catch (FileNotFoundException ex) {
            // System.out.println("[DEBUG] query_pairs.get(\"query\"): "+
            // query_pairs.get("query"));
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("The provided query is incorrect. Usage: /github?query=<query>\n");
            ex.printStackTrace();
          } catch (Exception ex) {
            System.out.println("[DEBUG] query_pairs.get(\"query\"): " + query_pairs.get("query"));
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("An unknown error occurred. Please make sure your GitHub URL is correct.");
            ex.printStackTrace();
          }
          // TODO: Parse the JSON returned by your fetch and create an appropriate
          // response based on what the assignment document asks for
          if (json != null && (json.charAt(0) == '[' || query_pairs.get("query").charAt(0) == 'u')) { // check that it is an array and query is for user repo
            try {
              // read the JSON file
              JSONArray arr = new JSONArray(json);
              // System.out.println(arr.toString(4));
              // Start the payload of the sent HTTP packet
              StringBuilder payload = new StringBuilder();
              payload
                  .append("<!DOCTYPE html>\n<html>\n<head>\n<title>Repository Information</title>\n</head>\n<body>\n");
              payload.append("<h1>Repository details:</h1>\n<ul>\n<ul>");

              for (int i = 0; i < arr.length(); i++) {
                JSONObject repo = arr.getJSONObject(i);
                // System.out.println(repo);
                String fullName = repo.get("full_name").toString();
                int id = Integer.parseInt(repo.get("id").toString());
                String login = repo.getJSONObject("owner").get("login").toString();
                /*
                 * HTML will look something like:
                 * <li>\n<strong>Full Name: </strong> fullname
                 */
                // TODO: add spacing between the bullet points
                payload.append("<li>\n<strong>Full Name:</strong> ").append(fullName)
                    .append("<br>\n<strong>ID:</strong> ").append(id)
                    .append("<br>\n<strong>Login:</strong> ").append(login)
                    .append("\n</li>\n\n");
              }
              //System.out.println("parsed information: " + payload);
              // send data to client
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append(payload + "</ul>");
            } catch (NullPointerException ex) {
              // bad coding practice here, maybe
              // we don't really need to do anything if json is null, one of those
              // catch blocks would have aleady sent the HTTP packet with some error code
              // just making sure the program doesn't crash because of it
              ex.printStackTrace();
            } catch(JSONException ex) {
              builder.setLength(0); // clear anything that might have been added to the builder before the exception
              // send a not supported :
              builder.append("HTTP/1.1 501 Not Implemented");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("Any request other than users/<user>/repos is not supported.");
            }
          } else {
            // json sent was not an array; wrong query made:
            // no instructions given for any other JSON parsing, so im just erroring out:
            // making sure another hasn't already filled out the packet:
            if(builder.length() == 0) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Only supported JSON parsing is through API requests that return arrays.");
            }

          }
        } else if(request.contains("babynames?")) {
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          query_pairs = splitQuery(request.replace("babynames?", ""));

          try {
            // parseBoolean() will return false on anything other than == true, so this input does not need to be validated
            boolean popular = Boolean.parseBoolean(query_pairs.get("popular"));
            String gender = query_pairs.get("gender");

            /*
             * if the key popular is not found, aka user mistyped the query:
             *    .get() will return null and parseBoolean will return false
             * if the key gender does not exist:
             *    .get() will also return null and gender will just be null.
             * 
             * So we need some custom checks here since no exception will directly be thrown
             * 
             * In essence, it almost doesn't matter what the user puts for popular or its value.
             * It will either be correct, be true, and evaluate to true, or evaluate to false, whether the query is correct or not.
             */

            if(gender == null || (!gender.equalsIgnoreCase("boy") && !gender.equalsIgnoreCase("girl"))) {
              throw new IllegalArgumentException(); // just go to catch block, no real logging here
            }

            // outsourcing to another API here: not using fethURL for auth
            URL url = new URL("https://api.api-ninjas.com/v1/babynames?gender=" + gender + "&popular=" + popular);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json"); // get response in JSON
            // yeah this should not be plain text
            // the account I made for this API service has no link to me and I used a temporary email, it really doesn't matter
            connection.setRequestProperty("X-Api-Key", "6hRqZUW/yaIDJXj682eV4g==JPA1Q4RVcBPDLjiV");
            connection.setRequestMethod("GET");
            connection.connect();
            StringBuilder names = null;
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
              InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
              BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
              names = new StringBuilder();
              String line;
              while ((line = bufferedReader.readLine()) != null) {
                names.append(line);
              }
              bufferedReader.close();
              System.out.println(names);
              JSONArray nameArr = new JSONArray(names.toString());
              Random rand = new Random();
              int idx = nameArr.length() == 0 ? 0 : rand.nextInt(nameArr.length() - 1);
  
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append(nameArr.getString(idx)); // returned array will have 10 names: 0-9
              System.out.println(nameArr.getString(idx));
            } else {
              System.out.println(connection.getResponseCode());
              builder.append("HTTP/1.1 501 Internal Server Error");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append("Something went wrong with NinjaAPI's services.");
            }
        
          } catch(IllegalArgumentException ex) {
            builder.setLength(0);
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("Please ensure passed parameters are valid: gender -> boy or girl, popular -> true or false.");
          } catch(IOException ex) {
            ex.printStackTrace();
          } catch(Exception ex) {
            ex.printStackTrace();
          }
        
        } else if(request.contains("convertcurrency?")){
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          query_pairs = splitQuery(request.replace("convertcurrency?", ""));

          try {
            System.out.println("amount: " + query_pairs.get("amount"));
            int amountToConvert = Integer.parseInt(query_pairs.get("amount"));
            String sourceCurrency = query_pairs.get("source");
            String targetCurrency = query_pairs.get("target");
            if(sourceCurrency == null || targetCurrency == null) {
              throw new IllegalArgumentException(); // go to catch block
            }

            // calculate the rate
            String pair = sourceCurrency + "_" + targetCurrency;
            System.out.println("pair: " + pair);
            // outsourcing the information here again:
            URL url = new URL("https://api.api-ninjas.com/v1/exchangerate?pair="+pair);
            //System.out.println(url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json"); // get response in JSON
            // yeah this should not be plain text
            // the account I made for this API service has no link to me and I used a temporary email, it really doesn't matter
            connection.setRequestProperty("X-Api-Key", "6hRqZUW/yaIDJXj682eV4g==JPA1Q4RVcBPDLjiV");
            connection.setRequestMethod("GET");
            connection.connect();
            //System.out.println(connection.getResponseCode());
            
            StringBuilder exchange_rate = null;
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
              // parse returned JSON
              InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
              BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
              exchange_rate= new StringBuilder();
              String line;
              while ((line = bufferedReader.readLine()) != null) {
                exchange_rate.append(line);
              }
              bufferedReader.close();
              System.out.println(exchange_rate);
              JSONObject returned = new JSONObject(exchange_rate);
              System.out.println(returned);
              double rate = returned.getDouble("exchange_rate");

              // we still have to do the math ourselves:
              double result = amountToConvert * rate;

              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n\n");
              builder.append(result);

            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
              throw new IllegalArgumentException();
            }

          } catch(NumberFormatException ex) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("Please ensure amount is a valid integer.");
          } catch(IllegalArgumentException ex) {
            builder.setLength(0);
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n\n");
            builder.append("Please ensure passed parameters are valid:");
            builder.append("amount -> valid integer\n");
            builder.append(" || source -> valid currency code");
            builder.append(" || target -> valid currency code");
          }

        }else {
          // if the request is not recognized at all
          //builder.setLength(0); // just in case
          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * 
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * 
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) throws IOException {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    /*
     * Changing this so that I can handle the excpetions under the if(github?)
     * section of the code
     * That's the only place this code is used anyway. I will handle it all there so
     * I can deal with
     * the server not crashing
     */
    // where old try block was
    URL url = new URL(aUrl);
    conn = url.openConnection();
    if (conn != null)
      conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
    if (conn != null && conn.getInputStream() != null) {
      in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
      BufferedReader br = new BufferedReader(in);
      if (br != null) {
        int ch;
        // read the next character until end of reader
        while ((ch = br.read()) != -1) {
          sb.append((char) ch);
        }
        br.close();
      }
    }
    // where old catch block was
    in.close();
    return sb.toString();
  }
}
