import java.io.*;
import java.util.*;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;
import org.apache.commons.fileupload.disk.*;

//written by Gracie Liu-Fang

public class eph extends HttpServlet {


	private void doRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        res.setContentType("text/html");

        PrintWriter out = res.getWriter();

        // filled into the form
        String selfUrl = res.encodeURL(req.getRequestURI());

        Connection dbh = null;
        try {
            printPageHeader(out);
            dbh = GracieDSN.connect("gfang_db");
            processForm(req,out,dbh,selfUrl);
        }
        catch (SQLException e) {
            out.println("Error: "+e);
        }
        catch (Exception e) {
            e.printStackTrace(out);
        }
        finally {
            close(dbh);
        }
        out.println("</body>");
        out.println("</html>");
    }

    private void printPageHeader(PrintWriter out) {
        out.println("<html>");
        out.println("<head>");

		out.println("<style>");

		out.println("</style>");
        out.println("<title>ephemeral</title>");
        out.println("<link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css' integrity='sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7' crossorigin='anonymous'>");
        out.println("<link rel='stylesheet' type='text/css' href='../css/eph.css'>");
        out.println("</head>");
        out.println("<body>");
    }

    //prints html for start page
    private void printStart(PrintWriter out, Connection dbh, String selfUrl) 
    	throws SQLException
    {
        out.println("<p style='margin-top: 15px;'> hi, fellow soul. welcome to ephermeral.</p>");
        out.println("<p> whether you're in your highest spirits or feeling like the world is against you</p>");
        out.println("<p> you're not alone.");

    	out.println("<h1>how are you feeling?</h1>");
    	out.println("<form method='post' action='"+selfUrl+"'>");
    	out.println("<input type='text' name='start-text' value=''>");
    	out.println("<br>");
        out.println("<input type='submit' class='btn btn-info' name='start-submit' value='share'>");
        out.println("<input type='submit' name='start-submit' value=\"don't want to share? no problem. click to see how others are doing around you\" ");
        out.println("style='background: none; border: none; display: block; margin-left: auto; margin-right: auto; text-decoration: none; cursor: pointer; color: #46b8da;'>");
        out.println("</form>");
    }
    //this is the start printed along with the posts
    private void printStartSupp(PrintWriter out, Connection dbh, String selfUrl) 
        throws SQLException
    {
        out.println("<p style='margin-top: 15px;'> hi, fellow soul. welcome to ephermeral.</p>");
        out.println("<p> whether you're in your highest spirits or feeling like the world is against you</p>");
        out.println("<p> you're not alone.");

        out.println("<h1>how are you feeling?</h1>");
        out.println("<form method='post' action='"+selfUrl+"'>");
        out.println("<input type='text' name='start-text' value=''>");
        out.println("<br>");
        out.println("<input type='submit' class='btn btn-info' name='start-submit' value='share'>");
        out.println("</form>");
    }


    private void processForm(HttpServletRequest request,
                             PrintWriter out,
                             Connection dbh,
                             String selfUrl)
    	throws SQLException, AmbiguousDataException 
	{
		String submit = request.getParameter("start-submit");
        String rx = request.getParameter("rx-submit");
        String postinapp = request.getParameter("inappropriate-post");
        String rxinapp = request.getParameter("inappropriate-rx");



		if(submit!=null) { //submit button pressed
			String text = request.getParameter("start-text");
			if(text!=null && text!=""){ //new post text input in start page empty
                try {
                    printStartSupp(out,dbh,selfUrl);
                    dashboard(out,dbh);
                    insertPost(out,dbh,selfUrl,text);
                    showGeneralPosts(out,dbh,selfUrl);
                }
                catch (AmbiguousDataException e) {
                    out.println("<p>Insertion did not work; " +
                        "Please try again, being more specific.");
                }
			} else { //no new posts, so just show general posts
                printStartSupp(out,dbh,selfUrl);
                dashboard(out,dbh);
                showGeneralPosts(out,dbh,selfUrl);
            }

        } else if(rx!=null) {
            String rx_text = request.getParameter("rx-text");
            if(rx_text!=null && rx_text!="") {
                String rx_pid = request.getParameter("rx_pid"); //get pid that rx belongs to
                if(rx_pid!=null) {
                    try {
                        printStartSupp(out,dbh,selfUrl);
                        dashboard(out,dbh);
                        insertRx(out,dbh,selfUrl,rx_text,rx_pid);
                        showGeneralPosts(out,dbh,selfUrl);
                    }
                    catch (AmbiguousDataException e) {
                        out.println("<p>Insertion did not work; " +
                            "Please try again, being more specific.");
                    }
                }
            }
        //someone marked a post as inappropriate
        } else if(postinapp!=null) {
            String inapp_pid = request.getParameter("inapp_pid");
            if(inapp_pid!=null) {
                printStartSupp(out,dbh,selfUrl);
                dashboard(out,dbh);
                markInaPost(out,dbh,inapp_pid);
                showGeneralPosts(out,dbh,selfUrl);
            } 
        } else if(rxinapp!=null) {
            String inapp_rid = request.getParameter("inapp_rid");
            if(inapp_rid!=null) {
                printStartSupp(out,dbh,selfUrl);
                dashboard(out,dbh);
                markInaReaction(out,dbh,inapp_rid);
                showGeneralPosts(out,dbh,selfUrl);
        } 
        //nothing needs to be processed, so print the start page
		} else {
            printStart(out,dbh,selfUrl);
        }
   
	} //end processForm

	/*writes the html to make a post with:
	1. enables rxs - call makeRX()
	2. mark as inappropriate
	3. show how many inappropriates
	*/
	private void makePost(PrintWriter out, Connection dbh, String selfUrl, String text, String time, String pid) 
        throws SQLException, AmbiguousDataException
        {
    		out.println("<div class='outer'>");

            out.println("<div class='post'>");

            out.println("<form method='post' action='"+selfUrl+"'>");
            out.println("<input type='hidden' name='inapp_pid' value="+pid+">");
            out.println("<div class='wrapper'>");
                out.println("<p>mark as inappropriate</p>");
                out.println("<button type='submit' class='inappropriate' name='inappropriate-post'>");
                out.println("<span class='glyphicon glyphicon-minus-sign'></span></button></div>");
            out.println("</form>");

    		out.println("<p class='text'>"+text+"</p>");
            out.println("<p>"+time);
    		out.println("</div>");

            showReactions(out,dbh,selfUrl,pid);

            out.println("</div>");
            out.println("</div>");

	   }
    //create one reaction given rid and its corresponding text
    private void makeRx(PrintWriter out, String selfUrl, String text, String rid) {
        out.println("<div class='rx'>");

        out.println("<form method='post' action='"+selfUrl+"'>");
        out.println("<input type='hidden' name='inapp_rid' value="+rid+">");
        out.println("<button type='submit' class='inappropriate' name='inappropriate-rx'>");
        out.println("<span class='glyphicon glyphicon-minus-sign'></span></button>");
        out.println("</form>");

        out.println("<p class='text'>"+text+"</p>");
        out.println("</div>");
    }

    //displays all reactions to a post given its pid
    private void showReactions(PrintWriter out, Connection dbh, String selfUrl, String pid)
        throws SQLException, AmbiguousDataException 
        {
            out.println("<div class='reactions'>");

            PreparedStatement rids = dbh.prepareStatement
                ("SELECT reaction.rid, text from reaction, rx_text where reaction.rid=rx_text.rid and reaction.pid=?");
            rids.setString(1,pid);

            ResultSet rs = rids.executeQuery();
            try {
                while(rs.next()) { //while there are still post ids
                    String rid = rs.getString("rid");
                    String text = rs.getString("text");
                    if(text!="" && text!=null) { //make sure has text
                        makeRx(out,selfUrl,text,rid);
                    }
                } //while
            }
            catch (SQLException e) {
            }
            out.println("</div>"); //end reactions
            rxTail(out,selfUrl,pid);
            
        } 
    //prints out input form for reaction
    private void rxTail(PrintWriter out, String selfUrl, String pid) {
        out.println("<div class='rx-tail'>");
        out.println("<form method='post' action='"+selfUrl+"'>");
        out.println("<input type='text' name='rx-text' value='' placeholder='share your reaction...'>");
        out.println("<button type='submit' name='rx-submit' style='display: none;'>");
        out.println("<input type='hidden' name='rx_pid' value="+pid+">");
        out.println("</form>");
        out.println("</div>");
    }
    
   
    //retreives num of inappropriate marks on a post;
    //increments if that num is less than 5
    //deletes the post and corresponding post_text otherwise

    //WARNING: still not functional yet; when page is refreshed all posts inc in inappr but that shouldn't happen
    private void markInaPost (PrintWriter out, Connection dbh, String pid) 
        throws SQLException 
        {
            //retrieve num of existing inappropriate votes
            PreparedStatement numIna = dbh.prepareStatement
                ("SELECT inappropriate FROM post where pid=?");
            numIna.setString(1,pid);

            ResultSet rs = numIna.executeQuery();
            if(rs.next()) {
                int num = rs.getInt(1);
                if(num!=-1) {
                    //delete post and corresponding post_text if number of inappropro votes surpasses 5
                    if(num>=5) {
                        PreparedStatement del = dbh.prepareStatement
                            ("DELETE from post where pid=?");
                        del.setString(1,pid);
                        del.execute();

                        PreparedStatement del_text = dbh.prepareStatement
                            ("DELETE from post_text where pid=?");
                        del_text.setString(1,pid);
                        del_text.execute();
                    } else {
                        PreparedStatement inc = dbh.prepareStatement
                            ("UPDATE post SET inappropriate=? where pid=?");
                        inc.setInt(1,num+1);
                        inc.setString(2,pid);
                        inc.execute();
                    }
                } else {
                    out.println("<p>can't get num of inappropriate marks... poop :( </p>");
                }
            } //end 1st if
        }
    private void markInaReaction(PrintWriter out, Connection dbh, String rid) 
        throws SQLException 
        {
            //retrieve num of existing inappropriate votes
            PreparedStatement numIna = dbh.prepareStatement
                ("SELECT inappropriate FROM reaction where rid=?");
            numIna.setString(1,rid);

            ResultSet rs = numIna.executeQuery();
            if(rs.next()) {
                int num = rs.getInt(1);
                if(num!=-1) {
                    //delete post and corresponding post_text if number of inappropro votes surpasses 5
                    if(num>=5) {
                        PreparedStatement del = dbh.prepareStatement
                            ("DELETE from reaction where rid=?");
                        del.setString(1,rid);
                        del.execute();

                        PreparedStatement del_text = dbh.prepareStatement
                            ("DELETE from rx_text where rid=?");
                        del_text.setString(1,rid);
                        del_text.execute();
                    } else {
                        PreparedStatement inc = dbh.prepareStatement
                            ("UPDATE reaction SET inappropriate=? where rid=?");
                        inc.setInt(1,num+1);
                        inc.setString(2,rid);
                        inc.execute();
                    }
                } else {
                    out.println("<p>can't get num of inappropriate marks... poop :( </p>");
                }
            } //end 1st if
        }

    //displays all posts in general board from latest
    private void showGeneralPosts(PrintWriter out, Connection dbh, String selfUrl)
        throws SQLException, AmbiguousDataException 
        {
            out.println("<div class='posts'>");

            Statement pids = dbh.createStatement();
            //also make sure that created_at is within 24 hours of current time
            String current_time = getTime();
            ResultSet rs = pids.executeQuery("select post.pid,created_at,text from post, post_text where post.pid = post_text.pid order by pid desc;");
            try {
                while(rs.next()) { //while there are still post ids
                    String pid = rs.getString("pid");
                    String time = rs.getString("created_at");
                    String text = rs.getString("text");
                    if(text!="" && text!=null) { //make sure has text
                        makePost(out,dbh,selfUrl,text,time,pid);
                    }
                } 
            }
            catch (SQLException e) {//while
                out.println("<p> there are no posts");
            }
            out.println("</div>"); //end posts
        } 

    //inserts input into post and post_text tables and makes post
    private void insertPost(PrintWriter out, Connection dbh, String selfUrl, String text) 
        throws SQLException, AmbiguousDataException
    {
        String time = getTime();
        PreparedStatement insert = dbh.prepareStatement 
            ("INSERT INTO post VALUES (null,'text',?,0)");
        insert.setString(1,time);
        insert.execute();

        //get PID of most recent; if exists insert into post_text
        Statement getPID = dbh.createStatement();
        ResultSet rs = getPID.executeQuery("SELECT last_insert_id()");
        if( ! rs.next() ) {
            out.println("<p> no max pid; can't save into db");
        } else {
            String pid = rs.getString(1);
            PreparedStatement insert_text = dbh.prepareStatement 
                ("INSERT INTO post_text VALUES (null,?,?)");
            insert_text.setString(1,pid);
            insert_text.setString(2,escape(text));
            insert_text.execute(); 

            // makePost(out,dbh,selfUrl,text,time,pid);
        }
    } //end insertPost

    //inserts reaction into databases reaction and rx_text
    private void insertRx(PrintWriter out, Connection dbh, String selfUrl, String text, String pid) 
        throws SQLException, AmbiguousDataException
        {
            String time = getTime();
            PreparedStatement insert = dbh.prepareStatement 
                ("INSERT INTO reaction VALUES (null,?,'text',0)");
            insert.setString(1,pid);
            insert.execute();

            //get RID of most recent; if exists insert into post_text
            Statement getRID = dbh.createStatement();
            ResultSet rs = getRID.executeQuery("SELECT last_insert_id()");
            if( ! rs.next() ) {
                out.println("<p> no max rid; can't save into db");
            } else {
                String rid = rs.getString(1);
                PreparedStatement insert_text = dbh.prepareStatement 
                    ("INSERT INTO rx_text VALUES (null,?,?)");
                insert_text.setString(1,rid);
                insert_text.setString(2,escape(text));
                insert_text.execute(); 

            }
        } //end insertTx

    

    /* has two boards: general and event
    topics exist in event boards only
    dashboard prints out general layout
    by default, general board is shown with regular posts
    clicking event button will show topics in side-tab format?
    each topic will have posts
    */
    private void dashboard(PrintWriter out, Connection dbh) {
        //out.println("<button type='button' name='event-btn' value='clicked' class='btn btn-default'>Events Board</button>");
       
        out.println("<h1> here's how others are feeling today: </h1>");

        // String event = request.getParameter("event-btn");
        // if(event!=null) {
        //     out.println("<p>event clicked</p>");
        // } else {
        //     out.println("<p>didn't work you idiot</p>");
        // }
    }



	private void close(Connection dbh) {
        if( dbh != null ) {
            try {
                dbh.close();
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
        }
    }



    private static String escape(String raw) {
        return StringEscapeUtils.escapeHtml(raw);
    }
    private static String getTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }

    // ================================================================

    // These are the entry points for HttpServlets
    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        doRequest(req,res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        doRequest(req,res);
    }



} //end


