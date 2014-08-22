package com.bye.project;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Resumable file upload:
 *
 * When a collection of files is about to be uploaded, Rad Upload will
 * send a zero byte request but the query string will have a parameter
 * named 'cmd' with a value of 0.
 *
 * The request may or may not include a hash code. If a hash code is
 * included, that means we are attempting to resume failed/paused
 * transfer of multiple files. Some files may have been completely
 * uploaded - some may have be partially uploaded.
 *
 * When a hash is not included this script will generate one and send
 * it back to the client. All future requests in this transfer need to
 * include this hash code in the query string.
 *
 * Before each file is uploaded, another empty request with a 'cmd' of
 * 1 has to be sent to the script. The script must then return the
 * number of bytes that have been previously written for that file. -1
 * will be the response if an error has occured. 0 will be returned if
 * this is the first attempt to upload the file in question.
 *
 * When all the files have been uploaded another zero byte request will
 * be made. This time cmd parameter will be set to 2 - meaning that we
 * have finished uploading the files. Now the script should send a
 * nicely formatted reply and move the files to a permanent location.
 *
 * Last but not least when the upload was interrupted we have another
 * response, in this case cmd=3
 *
 * <p>Copyright: Copyright (c) Rad Inks (Pvt) Ltd. 2008</p>
 * <p>Company: Rad Inks (Pvt) Ltd. </p>
 */
public class ResumableUploader extends HttpServlet
{

    String savePath = "/Users/ByeWebster/Desktop/TempFiles/";

    /**
     * Will attempt to locate all the uploaded files.
     * If the move flag is set, the files will be moved to a more permanent location.
     * Will also print out the name and the size of each file that has been uploaded.
     */
    protected void find_files(String hash, String path, PrintWriter out, boolean move)
    {

        try
        {

            String newPath = "/Users/ByeWebster/Desktop/TempFuck/";
            if (move)
            {
                /*
                 * Create a path name for the destination. The first call to
                 * This method will have "save_path/hash" as the path. incoming
                 * files are temporarily saved in the 'hash' folder inside the
                 * save path.
                 */
                int i = path.indexOf(hash);
                if (i == -1)
                {
                    //System.out.println("error hash not found in file path");
                    return;
                }
                newPath = path.substring(0, i);
                newPath += path.substring(i + hash.length());
            }

            /*
             * Get the list of files in this directory and process them
             */
            File f = new File(path);
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                File fw = files[i];
                if (!(fw.getName().equals(".") || fw.getName().equals("..")))
                {
                    File relPath = new File(path + File.separator + fw.getName());
                    File dest = new File(newPath + File.separator + fw.getName());

                    if (relPath.isDirectory())
                    {
                        if (move && !dest.isDirectory())
                        {
                            /*
                             * Create the directory tree.
                             */
                            dest.mkdirs();
                        }
                        /*
                         * Since this is a directory let's descend inside it and proess the files
                         * that are contained in it.
                         */
                        find_files(hash, relPath.getAbsolutePath(), out, move);
                    }
                    else
                    {
                        if (move)
                        {
                            if(i %2 == 0)
                            {
                                out.println("<tr bgcolor='#FFFF99'>");
                            }
                            else
                            {
                                out.println("<tr>");
                            }

                            /*
                             * Move this file to permanent storage.
                             */
                            out.println("<td>" + dest + "</td><td>" + relPath.length() + "</td></tr>");
                            relPath.renameTo(dest);
                        }
                    }
                    //System.out.println(i + " " + dest);
                }
            }
            if (move)
            {
                f.delete();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();

            /* retrieve the hash code */
            String hash = request.getParameter("hash");
            /* the command to perform */
            String cmd = request.getParameter("cmd");


            String fname = "";
            /* Retrieve the file name */
            if (request.getMethod().equals("PUT"))
            {
            }
            else
            {
                fname = request.getParameter("fname");
            }

            if (fname != null)
            {
                /* Strip out extra slashes at the start */
                if (fname.indexOf("//") != -1)
                {
                    fname = fname.substring(1);
                }

                /* Convert into relative paths */
                String userfile_parent = request.getParameter("userfile_parent");

                //System.out.println("userfile_parent ::" + request.getParameter("userfile_parent"));

                int i = fname.indexOf(userfile_parent);
                if (i == -1)
                {
                   // System.out.println("error hash not found in file path");
                    return;
                }

                fname = fname.substring(userfile_parent.length());
            }

            if (cmd != null)
            {
                /**
                 * Negotiations.
                 */

                System.out.println("cmd :" + cmd);


                if (cmd.equals("0"))
                {
                    if (hash == null || hash.equals(""))
                    {
                        /*
                         * generate a new hash code. For convinience it will be based on the
                         * session identifier.
                         */
                        hash = "0" + request.getSession().getId() + "1";
                    }
                    out.println(hash);
                    //System.out.println("hash = " + hash);
                    return;
                }
                else if (cmd.equals("1"))
                {
                    if (fname == null)
                    {
                        System.out.println("WARNING fname is not set");
                    }

                    System.out.println("File Path ::" + savePath + hash + File.separator + fname);

                    File f = new File(savePath + hash + File.separator + fname);
                    if (f.exists())
                    {
                        /*
                         * The file exists on the server. A previous upload has failed.
                         */
                        out.println(f.length());
                    }
                    else
                    {
                        out.println("0");
                    }
                }
                else if (cmd.equals("2"))
                {
                    printHeader(out);
                    //System.out.println("Saving files.");
                    find_files(hash, savePath + hash, out, true);
                }
                else if (cmd.equals("3"))
                {
                    //System.out.println("Interrupted not saving files ");

                    printHeader(out);
                    find_files(hash, savePath + hash, out, false);
                }
            }
            else
            {
                if (fname != null && !fname.equals(""))
                {
                    /*
                     * We are ready to write the file to the disk. First let's get a handle for
                     * reading the file data.
                     */
                    InputStream in = request.getInputStream();
                    String myPath = savePath + hash + File.separator;
                    File f = new File(myPath + fname);
                    File parent = f.getParentFile();

                    if (parent.exists())
                    {
                        if (!parent.isDirectory())
                        {
                            /* ouch */
                        }
                    }
                    else
                    {
                        /* create the path */
                        parent.mkdirs();
                    }

                    /* Find the seek position */
                    String offset = request.getParameter("offset");
                    //System.out.println("offset ************** :" + request.getParameter("offset"));
                    long seekPos = 0;
                    if (offset != null)
                    {
                        try
                        {
                            seekPos = Long.parseLong(offset);
                        } catch (NumberFormatException nex)
                        {
                            nex.printStackTrace();
                        }
                    }

                    /*
                     * Open a RandomAccessFile so that we can seek on it.
                     */
                    RandomAccessFile fout = new RandomAccessFile(f, "rw");
                    fout.seek(seekPos);
                    byte[] b = new byte[1024];

                    /*
                     * Read all the data and write to the disk.
                     */
                    while (true)
                    {
                        int i = in.read(b, 0, 1024);
                        if (i < 0)
                        {
                            break;
                        }
                        else
                        {
                            fout.write(b, 0, i);
                        }
                    }
                    fout.close();
                }
                else
                {
                    /*
                     * Check the save path.
                     */
                    try
                    {
                        File f = new File(savePath + "trysaveafile.txt");
                        FileOutputStream fout = new FileOutputStream(f);
                        fout.write("hello".getBytes());
                        fout.flush();
                        fout.close();
                        out.println("ok");
                    } catch (Exception ex)
                    {
                        out.println("The folder you have chosen cannot be written to");
                        ex.printStackTrace();
                    }
                }
            }

            out.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo()
    {
        return "Short description";
    }
    // </editor-fold>

    private void printHeader(PrintWriter out)
    {
        out.println("<html><head><title>Rad Upload</title></head>");
        out.println("<body  bgcolor='FFFFCC'>");
        out.println("<table border='0' cellpadding='5' width='100%' align='center'>");
        out.println("<tr><td colspan='2' bgcolor='#0066cc' align='center'><font color='#FFFFCC' size='+1' align='center'>Files Uploaded</font></td></tr>");
        out.println("<tr  bgcolor='#ffff00'><td><nobr>File Name</nobr></td>");
        out.println("<td align='right''><nobr>File size</nobr></td></tr>");
    }

    private void printFooter(PrintWriter out)
    {
        out.println("</table>");
        out.println("<p>&nbsp;</p>");

        out.println("<p style='text-align:center;'>PHP Upload handler provided by");
        out.println("<a href='http://www.radinks.com/upload/'>Rad Inks (Pvt) Ltd.</a></p>");
        out.println("<p>&nbsp;</p>");

        out.println("</body>");
        out.println("</html>");
    }
}
