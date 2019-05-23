package com.openfaas.function;

import com.openfaas.model.IHandler;
import com.openfaas.model.IResponse;
import com.openfaas.model.IRequest;
import com.openfaas.model.Response;

import java.io.StringWriter;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.ReasonerVocabulary;

import org.topbraid.shacl.rules.RuleUtil;
import org.topbraid.shacl.util.ModelPrinter;
import org.topbraid.shacl.validation.ValidationUtil;

public class Handler implements com.openfaas.model.IHandler 
{

    public IResponse Handle(IRequest req) 
    {
        // parse IRIs from query params    
        String schemaIRI = req.getQuery().get("schema");
        String graphIRI = req.getQuery().get("graph");

        // load schema and graph models from IRIs
        Model schemaModel = RDFDataMgr.loadModel(schemaIRI, Lang.TTL);
        Model graphModel  = RDFDataMgr.loadModel(graphIRI, Lang.TTL);

        // perform RDFS inferencing
        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        reasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel, ReasonerVocabulary.RDFS_FULL);
        InfModel infmodel = ModelFactory.createInfModel(reasoner, schemaModel, graphModel);
        String inferences = ModelPrinter.get().print(infmodel);

	    // execute SHACL rules
	    //Model inferReport = RuleUtil.executeRules(infmodel, infmodel, null, null);
	    //String rules = ModelPrinter.get().print(inferReport);

        // validate against SHACL shapes
	    Resource report = ValidationUtil.validateModel(infmodel, infmodel, true);
	    String validation = ModelPrinter.get().print(report.getModel());

        // write results to string
        //StringWriter sw = new StringWriter();
        //RDFDataMgr.write(sw, schemaModel, RDFFormat.NQ);

        // build response
        Response res = new Response();
	    res.setBody(inferences + "\n" + "\n" + validation);
        res.setContentType("text/turtle");

        return res;
    }
}
