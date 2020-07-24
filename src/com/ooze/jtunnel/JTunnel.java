package com.ooze.jtunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;

/**
 * Servlet implementation class JTunnel
 */
@WebServlet("/JTunnel")
public class JTunnel extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Boolean PROXY = false;
	private static final int BUFFER_SIZE = 4096; // 4KB
	static Boolean parser = false;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		CloseableHttpClient httpclient = null;
		try {
			if(PROXY) {
				HttpHost proxy = new HttpHost("127.0.0.1", 3128);
				DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

				CredentialsProvider provider = new BasicCredentialsProvider();
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("a69996", "$Maryline05");
				provider.setCredentials(AuthScope.ANY, credentials);	
				httpclient = HttpClients.custom().setDefaultCredentialsProvider(provider).setRoutePlanner(routePlanner).build();
				/*
 				httpclient = HttpClients.custom()
						.setDefaultAuthSchemeRegistry(provider)
						.setDefaultRequestConfig(config)
						//.setDefaultCredentialsProvider(provider)
						.setRoutePlanner(routePlanner)
						.build();
						*/
			} else {
				httpclient = HttpClients.createDefault();
			}

			// Get JTunnel page request
			String JTunnel = request.getRequestURL().toString();
			String URL = request.getParameter("page");

			// URL encoded ?
			boolean page64=false;
			if(URL==null || URL.length()==0) {
				URL = request.getParameter("page64");
				if (URL != null && URL.length() >0) {
					page64=true;
					byte[] decodedBytes = Base64.getDecoder().decode(URL);
					URL = new String(decodedBytes);
				}
			}

			// Swab binary file ?
			boolean fake=false;
			if(URL==null || URL.length()==0) {
				URL = request.getParameter("fake");
				if(URL==null || URL.length()==0) {
					URL = request.getParameter("fake64");
					byte[] decodedBytes = Base64.getDecoder().decode(URL);
					URL = new String(decodedBytes);
				}
				fake = true;
			}

			// Check if URL requested is valid
			URL u = new URL(URL);
			String domain = u.getProtocol() + "://" + u.getHost();
			CloseableHttpResponse httpclient_response = httpclient.execute(new HttpGet(URL));
			HttpEntity entity = httpclient_response.getEntity();

//			System.out.println("DEBUG page = " + URL);
//			System.out.println("DEBUG type : " + entity.getContentType());

			// Prepare answer
/*
			Header[] httpclient_header = httpclient_response.getAllHeaders();
			for (Header header : httpclient_header) {
				//response.setHeader(header.getName(), header.getValue());
				System.out.println(header.getName() +" - " + header.getValue());
			}
*/
			String encoding="";
			String contentType = entity.getContentType().getValue();
			if(fake) {
				response.setContentType("application/octet-stream");
				response.setHeader("Content-Disposition", "filename=\"ooze.swab\"");
			} else {
				response.setContentType(contentType);
				if(contentType.indexOf("charset=") !=-1)
					encoding = contentType.substring(contentType.indexOf("charset=")+8).toUpperCase();
				response.setHeader("Content-disposition", "filename=\"" + URL.substring(URL.lastIndexOf('/')+1) +  "\"");
			}
			if (entity != null) {
				// Serve text file
				if(entity.getContentType().getValue().indexOf("text") != -1 || entity.getContentType().getValue().indexOf("javascript") != -1) {
					if(encoding == "")
						encoding = "ISO-8859-1";
					String browser_response=EntityUtils.toString(entity,encoding);
	
					//System.out.println("DEBUG 0 :\n" + browser_response);
					if(!parser ) {
						// Pass 1  : global
						browser_response = browser_response.replaceAll("src='//", "src='http://");
						browser_response = browser_response.replaceAll("src=\"//", "src=\"http://");
						browser_response = browser_response.replaceAll("href='//", "href='http://");
						browser_response = browser_response.replaceAll("href=\"//", "href=\"http://");
						browser_response = browser_response.replaceAll("https://", JTunnel +"?page=https://");
						browser_response = browser_response.replaceAll("http://", JTunnel + "?page=http://");
	
						// If URL is targeting a page, then remove the end of the path
						if(!URL.equals(domain))
							URL = URL.substring(0,URL.lastIndexOf('/'));
	
						// Pass 2 : HTML
						browser_response = browser_response.replaceAll("href=\"/", "href=\"" + JTunnel + "?page=" + domain + "/");
						browser_response = browser_response.replaceAll("href=\'/", "href=\'" + JTunnel + "?page=" + domain + "/");
						browser_response = browser_response.replaceAll("src=\"/", "src=\"" + JTunnel + "?page=" + domain + "/");
						browser_response = browser_response.replaceAll("src=\'/", "src=\'" + JTunnel + "?page=" + domain + "/");
						browser_response = browser_response.replaceAll("href=\"(?!http)", "href=\"" + JTunnel + "?page=" + URL + "/");
						browser_response = browser_response.replaceAll("href=\'(?!http)", "href=\'" + JTunnel + "?page=" + URL + "/");
						browser_response = browser_response.replaceAll("src=\"(?!http)", "src=\"" + JTunnel + "?page=" + URL + "/");
						browser_response = browser_response.replaceAll("src=\'(?!http)", "src=\'" + JTunnel + "?page=" + URL + "/");
						browser_response = browser_response.replaceAll("href=\"(?!.*JTunnel)", "href=\"" + JTunnel + "?page=");
						browser_response = browser_response.replaceAll("href=\'(?!.*JTunnel)", "href=\'" + JTunnel + "?page=");
						browser_response = browser_response.replaceAll("src=\"(?!.*JTunnel)", "src=\"" + JTunnel + "?page=");
						browser_response = browser_response.replaceAll("src=\'(?!.*JTunnel)", "src=\'" + JTunnel + "?page=");
	
						// forms action
						browser_response = browser_response.replaceAll("action=\"(?!.*JTunnel)", "action=\"" + JTunnel + "?page=" + URL + "/");
						browser_response = browser_response.replaceAll("action=\'(?!.*JTunnel)", "action=\'" + JTunnel + "?page=" + URL + "/");
	
						// Pass 3 : CSS
						browser_response = browser_response.replaceAll("url\\(\"(?!.*JTunnel)", "url(\"" + JTunnel + "?page=" + URL + "/");
						browser_response = browser_response.replaceAll("url\\(\'(?!.*JTunnel)", "url(\'" + JTunnel + "?page=" + URL + "/");
						browser_response = browser_response.replaceAll("url\\((?!\"|'|.*JTunnel)", "url(" + JTunnel + "?page=" + URL + "/");
	
						// Background ?
	
						//System.out.println("DEBUG 1 :\n" + browser_response);
					} else {
						// Post process (issue with href="//a.fsdn.com/ without http://)
						browser_response = browser_response.replaceAll("src='//", "src='http://");
						browser_response = browser_response.replaceAll("src=\"//", "src=\"http://");
						browser_response = browser_response.replaceAll("href='//", "href='http://");
						browser_response = browser_response.replaceAll("href=\"//", "href=\"http://");
	
						// Parse and modify response
						String new_response=new String();
	
						// Populate list of keywords
						ArrayList<String> dico=new ArrayList<String>();
						dico.add(" src=");
						dico.add(" href=");
						dico.add(" action=");
						dico.add(":url(");
						dico.add(" url(");
	
						// Find first occurrence
						char sep=' ';
						String value=new String();
						int current_pos=0;
						String browser_response_lower = browser_response.toLowerCase(Locale.US);
						int pos=findFirst(browser_response_lower,dico);
						while(pos!=-1) {
							// Copy up to the keyword
							new_response = new_response + browser_response.substring(current_pos, current_pos + pos);
							current_pos = current_pos + pos;
							// Extract the keyword value and increase current position
							sep=browser_response.charAt(current_pos);
							if(sep == '"' || sep == '\'') {
								current_pos = current_pos + 1;
								value=browser_response.substring(current_pos, current_pos + browser_response.substring(current_pos).indexOf(sep));
								current_pos = current_pos + browser_response.substring(current_pos).indexOf(sep);
								new_response = new_response + sep;
							} else {
								if(browser_response.charAt(current_pos-1) == '(') {
									value=browser_response.substring(current_pos, current_pos + browser_response.substring(current_pos).indexOf(')'));
									current_pos = current_pos + browser_response.substring(current_pos).indexOf(')');									
								} else {
									value=browser_response.substring(current_pos, current_pos + browser_response.substring(current_pos).indexOf(' '));
									current_pos = current_pos + browser_response.substring(current_pos).indexOf(' ');
								}
							}
	
							// Modify the value
							new_response = new_response + tunnelize(JTunnel, URL, domain, page64, value);
	
							// Look for the next occurrence
							pos=findFirst(browser_response_lower.substring(current_pos),dico);
						}
						// Copy end of text
						new_response = new_response + browser_response.substring(current_pos);
	
						// Push response
						browser_response = new_response;
					}
	
					// Patch for what could not be processed
					browser_response = browser_response.replaceAll("\"http://(?!.*JTunnel)", "\"" +JTunnel + "?page=http://");
					browser_response = browser_response.replaceAll("\"https://(?!.*JTunnel)", "\"" + JTunnel + "?page=https://");
					browser_response = browser_response.replaceAll("'http://(?!.*JTunnel)", "'" +JTunnel + "?page=http://");
					browser_response = browser_response.replaceAll("'https://(?!.*JTunnel)", "'" + JTunnel + "?page=https://");
	
					// JTunnel response to browser
					// Update size
					response.setContentLengthLong(browser_response.length());
					//response.setCharacterEncoding("ISO-8859-1");
					PrintWriter	out=response.getWriter();
					out.print(browser_response);
				} else {
					// Serve binary file
	
					// Read entity content and send it to the browser
					if(entity.getContentLength()>0)
						response.setContentLengthLong(entity.getContentLength());
					InputStream instream = entity.getContent();
					ServletOutputStream output = response.getOutputStream();
					byte[] buffer = new byte[BUFFER_SIZE];
					int last_byte;
					int bytesRead;
					while ((bytesRead = instream.read(buffer)) != -1) {
						if(fake) {
							if(bytesRead % 2 == 0) {
								output.write(swab(buffer,bytesRead), 0, bytesRead);
							} else {
								output.write(swab(buffer,bytesRead-1), 0, bytesRead-1);
								last_byte = instream.read();
								if(last_byte != -1)
									output.write((byte)last_byte);
								output.write(buffer[bytesRead-1]);
							}
						} else {
							output.write(buffer, 0, bytesRead);
						}
					}
					output.close();
					instream.close();
				}
			}
			EntityUtils.consume(entity);
			httpclient_response.close();

		} catch(Exception ex) {
			ex.printStackTrace();
			PrintWriter	out=response.getWriter();
			out.println("<html><body><h3>JTunnel error :</h3>"+ex+"</body></html>");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public byte[] swab(byte[] buffer, int bytesRead) {
		byte[] buffer_swabbed = new byte[BUFFER_SIZE];
		// Swab bytes
		if(bytesRead % 2 == 0) {
			for(int i=0;i<bytesRead;i=i+2) {
				buffer_swabbed[i]=buffer[i+1];
				buffer_swabbed[i+1]=buffer[i];
			}
		} else {
			for(int i=0;i<bytesRead-1;i=i+2) {
				buffer_swabbed[i]=buffer[i+1];
				buffer_swabbed[i+1]=buffer[i];
			}
			buffer_swabbed[bytesRead-1] = buffer[bytesRead-1];
		}
		return buffer_swabbed;
	}

	public static int findFirst(String text, ArrayList<String> dico) {
		int first=-1;
		int temp=-1;
		for (int i=0;i<dico.size();i++) {
			temp=text.indexOf(dico.get(i));
			if(temp !=-1 && (first == -1 || temp<first))
				first = temp+dico.get(i).length();
		}
		return first;
	}

	// Tunnelize any URL / page
	public static String tunnelize(String JTunnel, String URL, String domain, boolean page64, String value) {
		//System.out.println("DEBUG Tunnelize / URL = " + URL + " - Domain = " + domain + " - Value = " + value);

		// Invalid value
		if(value == null || value.length() == 0 || value.indexOf("'") != -1 || value.indexOf("\"") != -1)
			return(value);

		// If URL is targeting a page, then remove the end of the path
		if(!URL.equals(domain))
			URL = URL.substring(0,URL.lastIndexOf('/'));

		String new_value=new String();
		if(value.charAt(0)=='/') {
			new_value= domain + value;
		} else {
			if(value.toLowerCase().indexOf("http") != -1) {
				new_value = value;
			} else {
				new_value = URL + "/" + value;
			}
		}

		String page="?page=";
		if(page64) {
			page="?page64=";
			new_value = JTunnel + page + Base64.getEncoder().encodeToString(new_value.getBytes());
		} else {
			new_value = JTunnel + page + new_value;
		}

		//System.out.println("DEBUG Value tunnelized = " + new_value);
		return(new_value);
	}

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

}