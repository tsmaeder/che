package org.eclipse.che.plugin.maven.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.ide.ext.java.shared.dto.Problem;
import org.eclipse.che.plugin.maven.server.core.reconcile.PomReconciler;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenTextDocumentService implements TextDocumentService {
    private static final Logger LOG               = LoggerFactory.getLogger(MavenTextDocumentService.class);

    private LanguageClient client;
    private PomReconciler reconciler;

    public MavenTextDocumentService(LanguageClient client, PomReconciler reconciler) {
        this.client= client;
        this.reconciler= reconciler;
    }

    @Override
    public CompletableFuture<CompletionList> completion(TextDocumentPositionParams position) {
        return null;
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return null;
    }

    @Override
    public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
        return null;
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams params) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams params) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        return null;
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
        return null;
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        return null;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String text = params.getTextDocument().getText();
        doReconcile(uri, text);
    }

    private void doReconcile(String uri, String text) {
        try {
            String projectPath= new File(new URI(uri)).getParent();
            List<Problem> problems = reconciler.reconcile(uri, projectPath, text);
            Map<Integer, Position> positions= mapPositions(text, problems);
            List<Diagnostic> diagnostics= problems.stream().map((Problem p)->convertProblem(positions, p)).filter(o->o != null).collect(Collectors.toList());
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
        } catch (ServerException | NotFoundException | URISyntaxException e) {
            LOG.error("Error reconciling content: "+uri, e);
        }
    }

    private Map<Integer, Position> mapPositions(String text, List<Problem> problems) {
        SortedSet<Integer> offsets= new TreeSet<>();
        for (Problem problem : problems) {
            offsets.add(problem.getSourceStart());
            offsets.add(problem.getSourceEnd());
        }
        Map<Integer, Position> result= new HashMap<>();
        int line= 0;
        int character= 0;
        int pos= 0;
        for (int offset : offsets) {
            while (pos < offset && pos < text.length()) {
                char ch= text.charAt(pos++);
                if (ch == '\r') {
                    if (text.charAt(pos) == '\n') {
                        pos++;
                    }
                    line++;
                    character=0;
                } else if (ch == '\n') {
                    line++;
                    character=0;
               } else {
                    character++;
                }
            }
            result.put(offset, new Position(line, character));
        };
        return result;
    }

    private Diagnostic convertProblem(Map<Integer, Position> positionMap, Problem problem) {
        Diagnostic result = new Diagnostic();
        Position start = positionMap.get(problem.getSourceStart());
        Position end = positionMap.get(problem.getSourceEnd());
        if (start == null || end == null) {
            LOG.error("Could not map problem range: "+problem);
            return null;
        }
        result.setRange(new Range(start, end));
        result.setMessage(problem.getMessage());
        result.setSeverity(problem.isError() ? DiagnosticSeverity.Error : DiagnosticSeverity.Warning);
        return result;
    }
    
    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        params.getContentChanges().stream().findAny().ifPresent(change->doReconcile(uri, change.getText()));
   }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        
    }

}
