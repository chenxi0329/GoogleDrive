/*
Todo:
    1.use profiller and calculate time complexity to optimize running speed
    2.handle edge case and invalid request
    3.rewrite uploading code to speed it up
*/

package gdrive;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.*;

public class Gdrive {
    //point to a folder where you have write access
    public static String workingPath="/opt";      
    // assume you mount hard drive under upLoadDrivePath directory.
    public static String upLoadDrivePath="/opt/X11/share/X11";    
    //a counter to count how many files we have uploaded
    public static int counter=0;
    public static void main(String[] args) throws IOException 
    {
        //prevent timeout error
        System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(100000));
        try (FileWriter writter = new FileWriter(workingPath+"/filesIndex.txt")) {
           Drive authKey=getAuthenKey();
           newDriveIn(writter,upLoadDrivePath,authKey);
        }
    }  
     //return a Authentication Key
    private static Drive getAuthenKey() throws IOException {
       String CLIENT_ID = "ID";
       String CLIENT_SECRET = "Secret";
       String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
       HttpTransport httpTransport = new NetHttpTransport();
         JsonFactory jsonFactory = new JacksonFactory();
         GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
        .setAccessType("online")
        .setApprovalPrompt("auto").build();
    String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
    System.out.println("Please open the following URL in your browser then type the authorization code:");
    System.out.println("  " + url);
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String code = br.readLine();
    GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
    GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);
    //Create a new authorized API client
     Drive service = new Drive.Builder(httpTransport, jsonFactory, credential).build();
     return service;
    }
    //return uploaded file ID
    private static String uploadAFileToDirectory(String path,String filename,String ParentID,Drive authKey) throws IOException {
    //Insert a file  
    File body = new File();
    body.setTitle(filename);
    body.setMimeType("*/*");
    body.setParents(Arrays.asList(new ParentReference().setId(ParentID)));
    java.io.File fileContent = new java.io.File(path+filename);
    FileContent mediaContent = new FileContent("*/*", fileContent);
    File file = authKey.files().insert(body, mediaContent).execute();
    counter++;
    System.out.println("Uploaded:" + path+filename);
    System.out.println(counter);
    return file.getId();
    }
    //return the root folder ID for new hard drive
    private static String logNewHardDrive(Drive authKey) throws IOException {
       System.out.println("Name Your New Hard Drive Folder");
       BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
       String name = br.readLine();
       FileWriter newJsp = new FileWriter(workingPath+"/"+name+".txt");
       newJsp.write("That should be hard drive info\n");
       newJsp.write("more hard drive info");
       newJsp.close();
       String rootFolderID=createFolder(name,"0",authKey);
       uploadAFileToDirectory(workingPath+"/",name+".txt",rootFolderID,authKey);
       return rootFolderID;
     }
    //return Folder ID of new folder created under given folder ID.
    private static String createFolder(String newFolderName,String ParentID,Drive authKey) throws IOException {
        File body = new File();
        body.setTitle(newFolderName);
        body.setMimeType("application/vnd.google-apps.folder");
        if(!ParentID.equals("0"))
        {
        body.setParents(Arrays.asList(new ParentReference().setId(ParentID)));
        }
        File rootFolderID = authKey.files().insert(body).execute();
        System.out.println("Created new Folder:"+newFolderName);
        return rootFolderID.getId();
    }
    //recursively upload all files 
    private static void enumerateFiles(String path, String folderID,FileWriter writter,Drive authKey) throws IOException {
        
            List<String> filesUnderFolder = new ArrayList<String>();
            List<String> foldersUnderFolder = new ArrayList<String>();
            foldersUnderFolder=listFolder(path);
            filesUnderFolder=listFile(path);
            for (int i = 0; i < filesUnderFolder.size(); i++) {
              writter.write(path+" "+filesUnderFolder.get(i)+" "+folderID+ "\n");
              uploadAFileToDirectory(path+"/",filesUnderFolder.get(i),folderID,authKey);
              }
            for (int i = 0; i < foldersUnderFolder.size(); i++) {
                String subFolderID=createFolder(foldersUnderFolder.get(i),folderID,authKey);
                enumerateFiles(path+"/"+foldersUnderFolder.get(i),subFolderID,writter,authKey);
              }
    }
    private static List<String> listFile(String path) {
        String files;
        java.io.File folder = new java.io.File(path);
        java.io.File[] listOfFiles = folder.listFiles(); 
        List<String> returnFiles = new ArrayList<String>();
        if(listOfFiles!=null)
        {
            for (int i = 0; i < listOfFiles.length; i++) 
            {
             if (listOfFiles[i].isFile()) 
             {
             files = listOfFiles[i].getName();
             returnFiles.add(files);
             }
            }
        }
        return returnFiles;
}
    private static List<String> listFolder(String path) {
        String files;
        java.io.File folder = new java.io.File(path);
        java.io.File[] listOfFiles = folder.listFiles(); 
        List<String> returnFiles = new ArrayList<String>();
        if(listOfFiles!=null)
        {
            for (int i = 0; i < listOfFiles.length; i++) 
            {
             if (listOfFiles[i].isDirectory()) 
             {
             files = listOfFiles[i].getName();
             returnFiles.add(files);
             }
            }
        }
        return returnFiles;
    }
    private static void newDriveIn(FileWriter writter, String upLoadDrivePath, Drive authKey) throws IOException {
            String rootFolderID=logNewHardDrive(authKey);
            enumerateFiles(upLoadDrivePath,rootFolderID,writter,authKey);    
    }
}
