import java.util.regex.Matcher
xmlPostfix=".xml"//指定要复制的xml的后缀
isMove = false // false is copy ,true is move
/**
 * 将批次识别的结果分拆开来
 */
def CopyXML(String srcDir, String dstDir,isMove) {
    File srcFile = new File(srcDir)
    new File(dstDir).mkdirs()
    File dstFile = new File("${dstDir}")
    srcFile.eachFileRecurse{ xml ->
	StringBuilder filePath = new StringBuilder(xml.getAbsolutePath())
	String xmlPath = filePath.replace(filePath.indexOf(srcFile.getAbsolutePath()),srcFile.getAbsolutePath().length(), "")
	File dstXml = new File(dstFile.getAbsolutePath() + "/" + xmlPath)
	dstXml.getParentFile().mkdirs()
	if (!xmlPath.endsWith(xmlPostfix)){
	 return;
	}
	if(isMove=="mv"){
		new File(srcDir, xmlPath).renameTo(new File(dstFile.getAbsolutePath() + "/",xmlPath))
	}else
	{
		dstXml.bytes = new File(srcDir, xmlPath).bytes
	}
         println "Process ${xml.getName()}"
	}
        
}

CliBuilder cli = new CliBuilder()
cli.f(longOpt: "from", args: 1, required: true, "source xml folder")
cli.t(longOpt: "to", args: 1, required: true, "split xml folder")
cli.a(longOpt: "action", args: 1, required: false, "mv(means move) or cp(means copy) source xml,default is cp")
OptionAccessor cmd = cli.parse(args)
if(cmd == null) {
    return
}
if ( (isMove = cmd.a) == "mv") {
	isMove=="mv"
}
CopyXML(cmd.f, cmd.t,isMove)
