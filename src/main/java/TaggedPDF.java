import debug.PrintTags;
import debug.VisualizeMarkedContent;
import exceptions.EmptyArgumentException;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.*;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This is an example on how to get a documents metadata information.
 *
 * @author Ben Litchfield
 *
 */
public class TaggedPDF {

    @Option(name = "-i", aliases = {"--input"}, required = true, metaVar = "PATH", usage = "specify a file")
    private String inArg;
    private File inputFile;
    private Path inputPath;

    @Option(name = "-o", aliases = {"--output"}, metaVar = "PATH", usage = "specify a directory for extracted data")
    private String outArg;
    private File outputFile;
    private Path outputPath;

    @Option(name = "-?", aliases = {"--help"}, usage = "show this message")
    private boolean help = false;

    public static void main( String[] args) throws IOException, CmdLineException {
        new TaggedPDF().run(args);
    }

    public void run(String[] args) throws CmdLineException, IOException {
        CmdLineParser parser = new CmdLineParser(this);

        parser.parseArgument(args);

        if (help) {
           parser.printUsage(System.err);
           System.exit(0);
        }

        throwIfEmpty(inArg);
        inputFile = new File(inArg);
        inputPath = inputFile.isFile() ? inputFile.getParentFile().toPath() : inputFile.toPath();

        if (isEmptyArg(outArg)) {
            outputFile = inputPath.resolve("output").toFile();
        } else {
            outputFile = new File(outArg);
        }

        outputFile.mkdirs();
        outputPath = outputFile.toPath();

        if (inputFile.isFile()) {
            processPDF(inputFile.toPath());
        } else {
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{pdf,PDF}");
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputPath, "*.pdf")) {
                for (Path file : directoryStream) {
                    System.out.println(file.getFileName());
                    if (matcher.matches(file.getFileName())) {
                        processPDF(file);
                    }
                }
            }
        }
    }


    private static void usage()
    {
        System.err.println( "Usage: java " + TaggedPDF.class.getName() + " <input-pdf>");
    }

    public void printMetadata(PDDocument document) throws IOException {
        PDDocumentInformation info = document.getDocumentInformation();

        File targetFile = new File("/home/sunveil/Documents/projects/ispras/src/tagged_pdf/resources/test.txt");
        OutputStream outStream = new FileOutputStream(targetFile);

        Map<PDPage, Map<Integer, PDMarkedContent>> markedContents = new HashMap<>();

        for (PDPage page : document.getPages()) {
            PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
            extractor.processPage(page);

            Map<Integer, PDMarkedContent> theseMarkedContents = new HashMap<>();
            markedContents.put(page, theseMarkedContents);
            for (PDMarkedContent markedContent : extractor.getMarkedContents()) {
                theseMarkedContents.put(markedContent.getMCID(), markedContent);
            }
        }

        PDStructureNode root = document.getDocumentCatalog().getStructureTreeRoot();
        showStructure(root, markedContents);

        PDPageTree allPages = document.getDocumentCatalog().getPages();

        for (int i = 0; i < allPages.getCount(); i++) {
            PDPage page = (PDPage) allPages.get(i);
            if (null != page) {
                InputStream contents = page.getContents();
                if (contents != null) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(contents));
                    String line = null;
                    while((line = in.readLine()) != null) {
                        line=line+"\n";
                        outStream.write(line.getBytes(StandardCharsets.UTF_8));
                        System.out.println(line);
                    }
                }
            }
        }

        IOUtils.closeQuietly(outStream);


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

    private void processPDF(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            Path debugDirPath = outputPath.resolve(".");
            VisualizeMarkedContent drawer = new VisualizeMarkedContent(document, debugDirPath);
            drawer.visualize(path.getFileName().toString());
            TaggedPDF meta = new TaggedPDF();
            //meta.printMetadata(document);
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

    void showStructure(PDStructureNode node, Map<PDPage, Map<Integer, PDMarkedContent>> markedContents) {
        String structType = null;
        PDPage page = null;

        if (node instanceof PDStructureElement) {
            PDStructureElement element = (PDStructureElement) node;
            structType = element.getStructureType();
            page = element.getPage();
        }
        Map<Integer, PDMarkedContent> theseMarkedContents = markedContents.get(page);
        System.out.printf("<%s>\n", structType);
        for (Object object : node.getKids()) {
            if (object instanceof COSArray) {
                for (COSBase base : (COSArray) object) {
                    if (base instanceof COSDictionary) {
                        showStructure(PDStructureNode.create((COSDictionary) base), markedContents);
                    } else if (base instanceof COSNumber) {
                        showContent(((COSNumber)base).intValue(), theseMarkedContents);
                    } else {
                        System.out.printf("?%s\n", base);
                    }
                }
            } else if (object instanceof PDStructureNode) {
                showStructure((PDStructureNode) object, markedContents);
            } else if (object instanceof Integer) {
                showContent((Integer) object, theseMarkedContents);
            } else if (object instanceof PDMarkedContentReference) {
                page = ((PDMarkedContentReference) object).getPage();
                theseMarkedContents = markedContents.get(page);
                showContent(((PDMarkedContentReference) object).getMCID(), theseMarkedContents);
            } else {
                System.out.printf("?%s\n", object);
            }
        }
        System.out.printf("</%s>\n", structType);
    }

    void showContent(int mcid, Map<Integer, PDMarkedContent> theseMarkedContents) {
        PDMarkedContent markedContent = theseMarkedContents != null ? theseMarkedContents.get(mcid) : null;
        List<Object> contents = markedContent != null ? markedContent.getContents() : Collections.emptyList();
        StringBuilder textContent =  new StringBuilder();
        for (Object object : contents) {
            if (object instanceof TextPosition) {
                textContent.append(((TextPosition)object).getUnicode());
            } else {
                textContent.append("?" + object);
            }
        }
        System.out.printf("%s\n", textContent);
    }

    private void throwIfEmpty(String arg) {
        if (isEmptyArg(arg)) {
            throw new EmptyArgumentException("A required option was not specified");
        }
    }

    private boolean isEmptyArg(String arg) {
        return arg == null || arg.isEmpty();
    }
}