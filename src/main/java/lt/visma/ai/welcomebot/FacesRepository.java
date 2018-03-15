package lt.visma.ai.welcomebot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class FacesRepository {

    private Map<String, Record> people = new HashMap<>();

	private int totalImageFileCount = 0;

	private String rootDirectory = null;
	
	private int lastId = 1;

    
    private static FilenameFilter SUBDIRECTORIES_FILTER = new FilenameFilter() {
        public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
        }
    };
    
    private static FilenameFilter PNG_FILTER = new FilenameFilter() {
        public boolean accept(File current, String name) {
            File f = new File(current, name);
            return (!f.isDirectory()) && f.getName().toLowerCase().endsWith(".png");
        }
    };
    
    private static FilenameFilter WAV_FILTER = new FilenameFilter() {
        public boolean accept(File current, String name) {
            File f = new File(current, name);
            return (!f.isDirectory()) && f.getName().toLowerCase().endsWith(".wav");
        }
    };
    
    private static class Record{
        Integer id;
        String  name;
        String  subDirectory;
        List<String> imageFiles;
        List<String> wavFiles;
		public boolean greeted = false;
        
        public static boolean isValidSubdir(String subdir) {
            int pos = filename(subdir).indexOf('.');
            return (pos >= 1 && pos < subdir.length()-1);
        }
        
        public Record(String subdir) {
            subDirectory = subdir.trim();
            
            String folderName = filename(subDirectory);
            
            int pos = folderName.indexOf('.');
            
            id = Integer.parseInt(folderName.substring(0, pos).trim());
            name= folderName.substring(pos + 1).trim();
            
            imageFiles = fileList(subdir,  new File(subDirectory).list(PNG_FILTER));
            wavFiles = fileList(subdir,  new File(subDirectory).list(WAV_FILTER));
        }
        
    }
    
    public FacesRepository(String dir) {
    	this.rootDirectory = dir;
    	
        init();
    }

	public Collection<String> getNames() {
        Set<String> knownPeople = people.keySet();
        ArrayList<String> ret = new ArrayList<>();
        ret.add("<add new...>");
        ret.addAll(knownPeople);
		return ret;
    }

    public Collection<Integer> getIds() {
        return people.entrySet().stream().map(e -> e.getValue().id).collect(Collectors.toSet());
    }

    public String getName(int id) {
        return people.entrySet().stream().filter(e -> e.getValue().id.equals(id)).map(e->e.getKey()).findFirst().orElse(null);
    }
    
    public Integer getId(String name) {
        Record record = people.get(name);
        if (record == null)
            return null;
        
        return record.id;
    }

	public int getImageCount() {
		return totalImageFileCount ;
	}
    
    public List<String> getImageFiles(int id) {
        return getImageFiles(getName(id));
    }
    
    public List<String> getImageFiles(String name) {
        Record record = people.get(name);
        if (record == null)
            return null;
        
        return record.imageFiles;
    }
    
    public List<String> getAudioFiles(String name) {
        Record record = people.get(name);
        if (record == null)
            return null;
        
        return record.wavFiles;
    }

	public boolean isGreeted(String name) {
		Record record = people.get(name);
		if (record == null)
			return false;
		
		return record.greeted ;
	}

	public void setGreeted(String name) {
		Record record = people.get(name);
		if (record == null)
			return;
		
		record.greeted = true;
	}

    private void init() {
        for (String subdir: listSubdirectories(rootDirectory)) {
            
            if (!Record.isValidSubdir(subdir))
                continue;
            
            add( new Record(subdir));
        }
    }
    
    private static List<String> listSubdirectories(String dir){
        String[] directories = new File(dir).list(SUBDIRECTORIES_FILTER);
        
        return fileList(dir, directories);
    }

    private static List<String> fileList(String dir, String[] fnames) {
        if (fnames == null || fnames.length == 0)
            return new ArrayList<>();
        
        List<String> ret= new ArrayList<>();
        for (String fname: fnames)
            ret.add(dir+"/"+fname);
        
        return ret;
    }
    
    private static String filename(String fullname) {
        int pos = fullname.lastIndexOf('/');
        int pos2 = fullname.lastIndexOf('\\');
        if (pos2<=pos)
            pos2 = pos;
        
        return fullname.substring(pos2+1);
    }
    
    private void add(Record record) {
        if (people.containsKey(record.name)) {
            System.out.println(record.name+" alrady exist");
            return;
        }
        
        Record oldRecord = people.values().stream().filter(v -> v.id == record.id).findFirst().orElse(null);
        if (oldRecord != null) {
            System.out.println(record.id+" already exists ("+oldRecord.name+")");
            return;
        }
        
        people.put(record.name, record);
        
        totalImageFileCount += record.imageFiles.size();
        
        this.lastId = (record.id >= lastId ? (record.id +1) : lastId);
    }
    
    public void dump() {
    	for (String name: getNames()){
    		System.out.println(name+": "+getId(name)+" -> "+getImageFiles(name));
    	}
	}

	public void save(BufferedImage capturedImage, String name) {
		if (name == null)
			return;
		
		Record record = people.get(name);
		if (record == null)
			record = createRecord(name);
		
		try {
			String pngFilename = record.subDirectory+"/"+(record.imageFiles.size()+1)+".png";
			File outputfile = new File(pngFilename);
			ImageIO.write(capturedImage, "png", outputfile);
			record.imageFiles.add(pngFilename);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private Record createRecord(String name) {
		int id = lastId ++ ;
		
		String newSubdir = rootDirectory + "/"+id + ". "+name;
		
		File newSubdirFile = new File(newSubdir);
		if (!newSubdirFile.exists())
			newSubdirFile.mkdirs();
		
		Record ret = new Record(newSubdir);
		
		people.put(name,  ret);
		
		return ret;
	}
    
    public static void main(String s[]) {
        new FacesRepository("C:\\deleteme\\faces");
    }
	
}
