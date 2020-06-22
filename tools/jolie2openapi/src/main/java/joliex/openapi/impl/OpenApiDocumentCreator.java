package joliex.openapi.impl;


import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import joliex.openapi.impl.support.OperationRestDescriptor;
import joliex.openapi.impl.support.RestDescriptor;
import joliex.openapi.impl.support.Utils;

import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class OpenApiDocumentCreator {

    String inputPort ;
    String outputDirectory ;
    String routerHost ;
    ProgramInspector inspector;
    private LinkedHashMap< String, TypeDefinition> typeMap;
    private LinkedHashMap< String, TypeDefinition > faultMap;
    private LinkedHashMap< String, TypeDefinition > subTypeMap;

    public OpenApiDocumentCreator(ProgramInspector inspector,  String inputPort , String outputDirectory, String routerHost) {

        this.inspector = inspector;
        this.outputDirectory = outputDirectory;
        this.inputPort = inputPort ;
        this.routerHost = routerHost;
    }

    public void ConvertDocument() throws FaultException {


        InputPortInfo[] inputPorts = inspector.getInputPorts();
        RestDescriptor restDescriptor = new RestDescriptor();
        restDescriptor.loadDescriptor("rest_template.json");
        Value errorValue = Value.create();
        Value documentValue = Value.create();
        for ( InputPortInfo inputPortInfo : inputPorts){
            if (inputPort.equals(inputPortInfo.id())){
                if  (!"sodep".equalsIgnoreCase(inputPortInfo.protocolId())){
                    errorValue.getChildren("wrongProtocol").add(Value.create(inputPortInfo.protocolId()));
                }

                Map<String, OperationDeclaration> operationsMap = inputPortInfo.operationsMap();

                operationsMap.forEach((operationName, operationDeclaration) -> {
                    if (operationDeclaration instanceof RequestResponseOperationDeclaration) {
                        try {
                            OperationRestDescriptor operationDescriptor = restDescriptor.getOperationRestDescriptor(operationName);
                            TypeDefinition requestType = ((RequestResponseOperationDeclaration) operationDeclaration).requestType();
                            inPathValue(operationDescriptor,requestType);

                        } catch (FaultException e) {
                             if ("OperationNoPresent".equals(e.faultName())){
                                 errorValue.getChildren("operationNotMapped").add(Value.create(operationName));
                             } else if ("ComplexTypeInPathParameter".equals(e.faultName())){
                                 errorValue.getFirstChild(operationName).getChildren("complexTypeInPathParameter").add(e.value());
                             } else if ("VoidTypeInPathParameter".equals(e.faultName())){
                                 errorValue.getFirstChild(operationName).getChildren("voidTypeInPathParameter").add(e.value());
                             }else if ("LinkInPathDefinition".equals())

                            errorValue.getChildren("operationNotMapped").add(Value.create(operationName));
                        }
                    }
                });
            }else{
                errorValue.getChildren("missingInputPort").add(Value.create(inputPort));
            }
        }
        if (errorValue.hasChildren()){
            throw new FaultException ("RestApiMappingError", errorValue);
        }
    }


    private Value inPathValue(OperationRestDescriptor operationRestDescriptor , TypeDefinition typeDefinition ) throws FaultException {
        Value value = Value.create();
        if( !NativeType.isNativeTypeKeyword( typeDefinition.id() )){
            if( Utils.hasSubTypes( typeDefinition ) ) {
                Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( typeDefinition );
                Iterator i = supportSet.iterator();
                while( i.hasNext() ) {
                    Map.Entry me = (Map.Entry) i.next();

                    System.out.println(((TypeDefinition) me.getValue()).id() + "\n");
                    if (operationRestDescriptor.isInPath(((TypeDefinition) me.getValue()).id())) {
                        if (((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id()).getFirstChild("message").setValue("In path element need to be a native types not typeLink");
                            throw new FaultException("LinkInPathDefinition" , typeErrorValue);
                        }else if (Utils.hasSubTypes((TypeDefinition) me.getValue())){
                            Value typeErrorValue = Value.create();
                            typeErrorValue.getFirstChild(((TypeDefinition) me.getValue()).id()).getFirstChild("message").setValue("In path element need to be a native types not complexType");
                            throw new FaultException("LinkInPathDefinition" , typeErrorValue);
                        }



                    }
                }
            }
        }
        return value;
    }



    private void parseSubType( TypeDefinition typeDefinition ) {
        if( Utils.hasSubTypes( typeDefinition ) ) {
            Set< Map.Entry< String, TypeDefinition > > supportSet = Utils.subTypes( typeDefinition );
            Iterator i = supportSet.iterator();
            while( i.hasNext() ) {
                Map.Entry me = (Map.Entry) i.next();
                // System.out.print(((TypeDefinition) me.getValue()).id() + "\n");
                if( ((TypeDefinition) me.getValue()) instanceof TypeDefinitionLink) {
                      System.out.println(me.getKey());
                }
            }
        }
    }

}
