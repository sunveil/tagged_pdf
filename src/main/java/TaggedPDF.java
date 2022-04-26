import debug.VisualizeMarkedContent;
import exceptions.EmptyArgumentException;
import model.TaggedDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.*;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.TextPosition;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.image.RenderedImage;
import java.io.*;
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

    private void processPDF(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            Path debugDirPath = outputPath.resolve(".");
            TaggedDocument doc = new TaggedDocument(document, debugDirPath);
            doc.parseTags(path.getFileName().toString());
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
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