import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * This is an example on how to get a documents metadata information.
 *
 * @author Ben Litchfield
 *
 */
public class TaggedPDF
{

    public static void main( String[] args ) throws IOException
    {
        if( args.length != 1 )
        {
            usage();
        }
        else
        {
            try (PDDocument document = Loader.loadPDF(new File(args[0])))
            {
                TaggedPDF meta = new TaggedPDF();
                meta.printMetadata( document );
            }
        }
    }

    private static void usage()
    {
        System.err.println( "Usage: java " + TaggedPDF.class.getName() + " <input-pdf>");
    }

    public void printMetadata(PDDocument document) throws IOException {
        PDDocumentInformation info = document.getDocumentInformation();
        PDDocumentCatalog cat = document.getDocumentCatalog();
        PDStructureTreeRoot treeRoot = document.getDocumentCatalog().getStructureTreeRoot();
        PDPageTree allPages = document.getDocumentCatalog().getPages();
        PDPage page = (PDPage) allPages.get(0);
        InputStream contents = page.getContents();
        if (contents != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(contents));
            String line = null;
            File targetFile = new File("/home/sunveil/Documents/projects/ispras/src/tagged_pdf/resources/test.txt");
            OutputStream outStream = new FileOutputStream(targetFile);

            while((line = in.readLine()) != null) {
                line=line+"\n";
                outStream.write(line.getBytes(StandardCharsets.UTF_8));
                System.out.println(line);
            }
            IOUtils.closeQuietly(outStream);
        }
        System.out.println( "Page Count=" + document.getNumberOfPages() );
        System.out.println( "Title=" + info.getTitle() );
        System.out.println( "Author=" + info.getAuthor() );
        System.out.println( "Subject=" + info.getSubject() );
        System.out.println( "Keywords=" + info.getKeywords() );
        System.out.println( "Creator=" + info.getCreator() );
        System.out.println( "Producer=" + info.getProducer() );
        System.out.println( "Creation Date=" + formatDate( info.getCreationDate() ) );
        System.out.println( "Modification Date=" + formatDate( info.getModificationDate() ) );
        System.out.println( "Trapped=" + info.getTrapped() );
        PrintTags printer = new PrintTags();
        int pageNum = 0;
        for(PDPage p: document.getPages()) {
            pageNum++;
            System.out.println( "Processing page: " + pageNum );
            printer.processPage(p);
        }
    }

    private String formatDate(Calendar date) {
        String retval = null;
        if(date != null) {
            SimpleDateFormat formatter = new SimpleDateFormat();
            retval = formatter.format( date.getTime() );
        }
        return retval;
    }
}