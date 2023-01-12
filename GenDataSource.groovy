/**
 * 生成统一格式的数据源(即业务维度文件放在文件夹中，并使用.dim作为后缀，并添加flag.new标记)
 */
import java.text.SimpleDateFormat

SrcDir = ""
DestDir = ""
UseDate = new Date()
Batch = -1
DateFormator = new SimpleDateFormat("yyyy-MM-dd")

// 从指定目录枚举所有的wav文件
def EnumWavFile(String path) {
    File dir = new File(path)
    if (!dir.exists())
        return null


    def wavList = []
    dir.eachFileRecurse { file ->
        if (file.getPath() ==~ /.+\.(?i:wav|vox|v3)$/) {
            StringBuilder filePath = new StringBuilder(file.getAbsolutePath())
            filePath.replace(
                    filePath.indexOf(dir.getAbsolutePath()),
                    dir.getAbsolutePath().length(), "")

            wavList.add(filePath.toString())
        }
    }

    return wavList
}

//将语音分批次，batch代表一个批次的大小
def GenBatchSource(destDir, wavList, date, isMove, batch) {
    if (batch > wavList.size) {
        println("config error : Batch size greater than speech count !")
        return;
    }
    def dateStr = DateFormator.format(date)

    //先将语音按照批次大小拆分
    List<String[]> list = new ArrayList<String[]>();
    def wavBath = []
    for (int c = 0; c < wavList.size(); c++) {
        if (wavBath.size() < batch) {
            wavBath.add(wavList[c]);
            if (wavBath.size() == batch) {
                list.add(wavBath);
                wavBath = [];
            }
        }
    }

    if (wavBath.size() != null && wavBath.size() != 0) {
        list.add(wavBath);
    }

    def i = 0;
    def j = 0;
    for (int c = 0; c < list.size(); c++) {
        batch = list.get(c);
        new File("$destDir/Speech/$dateStr-$c/file").mkdirs()
        new File("$destDir/Dimension/$dateStr-$c").mkdirs()

        File speechDimFile = new File("$destDir/Speech/$dateStr-$c/${dateStr}.dim")
        File dimensionFile = new File("$destDir/Dimension/$dateStr-$c/${dateStr}.dim")
        File speechFileDir = new File("$destDir/Speech/$dateStr-$c/file")

        dimensionFile.withWriter { out ->
            out.println("任务流水号|录音列表")
            batch.size().times {
                out.println("$i|$i")
                i++;
            }
        }

        new File("$destDir/Dimension/$dateStr-$c" + "/flag.new").createNewFile()

        speechDimFile.withWriter { out ->
            out.println("电话流水号|FilePath")

            batch.each { wavPath ->
                File srcSpeech = new File("file", wavPath)
                // 如果识别结果存在，也一并复制过去
                File srcXMLFile = new File(srcSpeech.getAbsolutePath() + ".xml");

                out.println("$j|${srcSpeech}")

                // 把语音复制过去
                File destSpeech = new File(speechFileDir.getAbsolutePath() + "/" + wavPath)
                File destXMLFile = new File(destSpeech.getAbsolutePath() + ".xml");
                destSpeech.getParentFile().mkdirs()

                if (isMove) {
                    File rawVoiceFile = new File(SrcDir, wavPath);
                    rawVoiceFile.renameTo(destSpeech);

                    if (srcXMLFile.exists()) {
                        srcXMLFile.renameTo(destXMLFile);
                    }
                } else {
                    destSpeech.bytes = new File(SrcDir, wavPath).bytes
                    if (srcXMLFile.exists()) {
                        destXMLFile.bytes = srcXMLFile.bytes;
                    }
                }

                j++
            }

            new File("$destDir/Speech/$dateStr-$c" + "/flag.new").createNewFile()
        }
    }
}

// 生成语音数据到指定目录
def GenerateSource(destDir, wavList, date, isMove) {
    def dateStr = DateFormator.format(date)

    new File("$destDir/Speech/$dateStr/file").mkdirs()
    new File("$destDir/Dimension/$dateStr").mkdirs()

    File speechDimFile = new File("$destDir/Speech/$dateStr/${dateStr}.dim")
    File dimensionFile = new File("$destDir/Dimension/$dateStr/${dateStr}.dim")
    File speechFileDir = new File("$destDir/Speech/$dateStr/file")

    dimensionFile.withWriter { out ->
        out.println("任务流水号|录音列表")
        wavList.size().times { i ->
            out.println("$i|$i")
        }
    }

    new File("$destDir/Dimension/$dateStr" + "/flag.new").createNewFile()

    speechDimFile.withWriter { out ->
        out.println("电话流水号|FilePath")

        def i = 0
        wavList.each { wavPath ->
            File srcSpeech = new File("file", wavPath)
            out.println("$i|${srcSpeech}")

            // 把语音复制过去
            File destSpeech = new File(speechFileDir.getAbsolutePath() + "/" + wavPath)
            destSpeech.getParentFile().mkdirs()
            if (isMove) {
                File rawVoiceFile = new File(SrcDir, wavPath);
                rawVoiceFile.renameTo(destSpeech);
            } else {
                destSpeech.bytes = new File(SrcDir, wavPath).bytes
            }
            ++i
        }

        new File("$destDir/Speech/$dateStr" + "/flag.new").createNewFile()
    }
}

CliBuilder cli = new CliBuilder()
cli.f(longOpt: "from", args: 1, required: true, "raw speech folder")
cli.t(longOpt: "to", args: 1, required: true, "data source save folder")
cli.d(longOpt: "date", args: 1, required: false, "date of data source for importing")
cli.m(longOpt: "move", args: 1, required: false, "is move raw speech ? default copy raw speech to data source folder")
cli.b(longOpt: "batch", args: 1, required: false, "batch size")

OptionAccessor cmd = cli.parse(args)
if (cmd == null) {
    return
}

SrcDir = cmd.f
DstDir = cmd.t
if (cmd.d) {
    UseDate = DateFormator.parse(cmd.d)
}
if (cmd.b) {
    Batch = Integer.parseInt(cmd.b)
}

println "Source Directory: ${SrcDir}"
println "Destination Directory: ${DstDir}"
println "Used Date: ${DateFormator.format(UseDate)}"
println ""

println "Generating for ${DateFormator.format(UseDate)}"

if (Batch <= 0) {
    GenerateSource(
            DstDir,
            EnumWavFile(SrcDir),
            UseDate,
            cmd.m)
} else {
    GenBatchSource(
            DstDir,
            EnumWavFile(SrcDir),
            UseDate,
            cmd.m,
            Batch)
}

println("Finish !")
