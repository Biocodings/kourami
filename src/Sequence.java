/*
Part of Kourami HLA typer/assembler
(c) 2017 by  Heewook Lee, Carl Kingsford, and Carnegie Mellon University.
See LICENSE for licensing.
*/
import java.util.*;

public class Sequence{

    private ArrayList<Base> seq;
    private String alleleName; //allele name: ex) A:01:01:01:01
    private StringBuffer columnSequence; //columnsSequence is a string containing all the paddings to match the length of MSA
    private StringBuffer fullSequence;   //fullSequence is 5'-3' DNA string without any paddings. So it's either equal to or shorter than columnSequence.
    private int[] boundaries;//contains start position of each boundary, alternating between intron/exon
    private int[] segmentOffsets;

    private int[] cumulativeOffsets;

    /*this is used to get the next colPosition for base. skipping over all gaps*/
    public int getNextColPosForBase(int colpos){
	try{
	    
	    if(seq.get(colpos).isBase())
		return colpos;
	    int i = colpos;
	    for( ; i < seq.size();i++){
		if(seq.get(i).isBase())
		    return i;
	    }
	    return i;
	}catch(Exception e){
	    e.printStackTrace();
	    System.err.println(this.alleleName);
	    System.exit(1);
	}
	return -100;
    }
    /*
    public void insertBlanks(int pos, ArrayList<Base> insBases){
	this.seq.addAll(pos, insBases);
	}*/

    //pos is 0-based index
    public void insertBlanks(int pos, int len){
	StringBuffer blanks = new StringBuffer();
	for(int i=0;i<len;i++){
	    blanks.append(".");
	}
	columnSequence.insert(pos, blanks);
	this.insertBases(pos, len);
    }
    
    //pos is 0-based index where insertion should happen
    private void insertBases(int pos, int len){
	Base preBase = this.seq.get(pos - 1);
	int preBaseBP = preBase.getBasePos();
	int preBaseCP = preBase.getColPos();
	int preBaseB2CO = preBase.base2colOffset() + 1;
	boolean preBaseExonFlag = preBase.isExon();
	int preBaseIEN = preBase.getIntronExonNumber();
	ArrayList<Base> bases = new ArrayList<Base>();
	for(int i=0; i<len; i++){
	    bases.add(new Base('.', preBaseBP, preBaseCP, preBaseB2CO, preBaseExonFlag, preBaseIEN));
	    preBaseCP++;
	    preBaseB2CO++;
	}
	this.seq.addAll(pos, bases);
	
	int boundaryIndex = this.getBoundaryIndexFromIntronExonNumber(preBaseExonFlag, preBaseIEN)+1;
	this.segmentOffsets[boundaryIndex - 1] += len; // update current block(intron or exon)'s offset
	//update boundaryies
	for(int i=boundaryIndex; i<this.boundaries.length; i++){
	    this.boundaries[i] += len;
	}
	//update cumulativeOffsets
	for(int i=boundaryIndex-1;i<this.cumulativeOffsets.length;i++){
	    this.cumulativeOffsets[boundaryIndex] += len;
	}

    }

    //    IENum   boundariesIndex
    //    1 (I)             0         
    //    1 (E)             1
    //    2 (I)             2
    //    2 (E)             3         
    //    3 (I)             4
    //    3 (E)             5
    public int getBoundaryIndexFromIntronExonNumber(boolean isExon, int ien){
	if(isExon)
	    return ien*2-1;
	else //intron
	    return ien*2-2;
    }


    public void verify(){
	boolean flag = true;
	int pbp = 0;
	int pcp = 0;
	StringBuffer bf = new StringBuffer( this.alleleName  + "\tNOT VERIFIED\n");
	//HLA.log.appendln("Verifying:\t" + this.alleleName);
	for(int i=0; i<this.seq.size(); i++){
	    if(Base.isBase(this.seq.get(i).getIBase())){
		pbp++;
		pcp++;
	    }else if(Base.isGap(this.seq.get(i).getIBase())){
		pcp++;
	    }else{
		bf.append("**************UNKNOWN SYMBOL!!!!\n" + this.seq.get(i).getBase() + ":" +this.seq.get(i).getIBase());
	    }
	    //boolean flag = true;
	    if(flag && this.seq.get(i).getBasePos() != pbp){
	        bf.append("Base position doesn't match:\t[predicted, recorded] = [" + pbp + "," + this.seq.get(i).getBasePos()+"]");
		flag = false;
	    }
	    if(flag && this.seq.get(i).getColPos() != pcp){
		bf.append("Col  position doesn't match:\t[predicted, recorded] = [" + pcp + "," + this.seq.get(i).getColPos()+"]");
		flag = false;
	    }
	}
	//HLA.log.appendln(this.columnSequence);
	if(flag != true){
	    HLA.log.appendln(bf.toString());
	    HLA.log.appendln(this.columnSequence.toString() + ":(CS)");
	    HLA.log.appendln(this.fullSequence.toString() + ":(BS)");
	}//else{
	// HLA.log.appendln(this.alleleName + "\tVERIFIED.");
	//}
	
    }

    public String getAlleleName(){
	return this.alleleName;
    }
    
    public int getColLength(){
	return this.seq.size();
    }
    
    public Base baseAt(int i){
	return this.seq.get(i);
    }

    public String toFastaString(){
	StringBuffer bf = new StringBuffer(">" + this.alleleName + "\n");
	bf.append(this.fullSequence.toString() + "\n");
	return bf.toString();
    }

    public String toFastaColumnSequence(){
	StringBuffer bf = new StringBuffer(">" + this.alleleName + "\n");
	bf.append(this.columnSequence.toString() + "\n");
	return bf.toString();
    }

    public void printPositions(){
	for(int i=0; i<this.seq.size(); i++){
	    Base b = this.seq.get(i);
	    HLA.log.append(b.getBase() + "["+b.getBasePos()+","+b.getColPos() + "]\t");
	}
	HLA.log.appendln();
    }
    
    public void printPositions(int colPosIndex, int l){
	for(int i=colPosIndex; i<colPosIndex+l && i<this.seq.size(); i++){
	    Base b = this.seq.get(i);
	    if(i==colPosIndex)
		HLA.log.append("["+b.getBasePos()+","+b.getColPos() + "," + colPosIndex + "]\t");
	    HLA.log.append(b.getBase());
	}
	HLA.log.appendln();
    }


    //basePos <= colPos in MSA
    public int getColPosFromBasePos(int basePos){
	/*
	HLA.log.appendln("Accessing:\t" + this.alleleName);
	HLA.log.appendln("ColLen:\t" + this.columnSequence.length() + "\tBaseLen:\t" + this.fullSequence.length());
	HLA.log.appendln("fullSequence: " + fullSequence.toString());
	HLA.log.appendln("colmSequence: " + this.getColumnSequence());
	*/
	if(this.alleleName.equals("DRB3*01:12")){
	    System.err.println("Accessing:\t" + this.alleleName);
	    System.err.println("ColLen:\t" + this.columnSequence.length() + "\tBaseLen:\t" + this.fullSequence.length());
	    System.err.println("fullSequence: " + fullSequence.toString());
	    System.err.println("colmSequence: " + this.getColumnSequence());
	}
	Base b = null;
	for(int p = basePos; p<=columnSequence.length(); p++){
	    b = this.seq.get(p-1);
	    if(this.alleleName.equals("DRB3*01:12"))
		System.err.println((p-1) + "\t" + b.getBasePos() + "\t" + basePos + "\t" + b.getColPos());
	    //HLA.log.appendln((p-1) + "\t" + b.getBasePos() + "\t" + basePos);
	    if(b.getBasePos() == basePos){
		//HLA.log.appendln();
		return b.getColPos();
	    }
	}
	//HLA.log.appendln("BAD");
	return -1;
    }
    
    public int getBasePosFromColPos(int colPos){
	return this.seq.get(colPos-1).getBasePos();
    }

    public Sequence(){
	this.seq = new ArrayList<Base>();
	this.alleleName = null;
	this.columnSequence = new StringBuffer();
	this.fullSequence = new StringBuffer();
	this.boundaries = null;
	this.segmentOffsets = null;
	this.cumulativeOffsets = null;
    }

    public void printNthBoundary(int n){
	HLA.log.append(this.alleleName + "\t");
	if(n < this.boundaries.length-1)
	    HLA.log.appendln(this.columnSequence.substring(this.boundaries[n]-1 + this.boundaries[n+1]-1));
	else
	    HLA.log.appendln(this.columnSequence.substring(this.boundaries[n]-1));
    }

    public void printBoundaries(){
	for(int i=0;i<this.boundaries.length; i++){
	    HLA.log.appendln("boundaries("+i+"):" + boundaries[i] + "," + (i==(this.boundaries.length-1) ? "END" : boundaries[i+1]));
	    HLA.log.appendln("segmentOff("+i+"):" + segmentOffsets[i] );
	    HLA.log.appendln("cumulatOff("+i+"):" + cumulativeOffsets[i] );
	}
    }

    public boolean withinTypingRegion(Node n, ArrayList<int[]> typingIntervals){
	int colNum =  n.getColIndex();
	for(int[] intv : typingIntervals){
	    if(colNum > intv[0] && colNum < intv[1])
		return true;
	}
	return false;
    }

    public String getTypingSequence(){
	StringBuffer bf = new StringBuffer();
	if(this.isClassI()){//CLASS I : exon 2 and exon 3
	    bf.append(Bubble.stripPadding(columnSequence.substring(this.boundaries[3]-1, this.boundaries[4]-1)));
	    bf.append(Bubble.stripPadding(columnSequence.substring(this.boundaries[5]-1, this.boundaries[6]-1)));
		
	}else{//CLASS II : exon 2 only
	    bf.append(Bubble.stripPadding(columnSequence.substring(this.boundaries[3]-1, this.boundaries[4]-1)));
	}
	return bf.toString();
    }
    

    public boolean withinTypingExon(int colNum){
	if(this.isClassI()){//CLASS I : exon 2 and 3
	    if( 
	       (colNum >= this.boundaries[3] && colNum < this.boundaries[4])
	       || (colNum > this.boundaries[5] && colNum < this.boundaries[6])
	       //|| (colNum > this.boundaries[7] && colNum < this.boundaries[8])
		)
		return true;
	}else{//CLASS II : only typing exon 2
	    if( 
	       (colNum >= this.boundaries[3] && colNum < this.boundaries[4])
		)
		return true;
	}
	return false;
    }
    
    public boolean isClassI(){
	if(this.alleleName.startsWith("A") || this.alleleName.startsWith("B") || this.alleleName.startsWith("C")
	   || this.alleleName.startsWith("F") || this.alleleName.startsWith("G") || this.alleleName.startsWith("H")
	   || this.alleleName.startsWith("J") || this.alleleName.startsWith("K") || this.alleleName.startsWith("L")
	   || this.alleleName.startsWith("V"))
	    return true;
	return false;
    }

    public String getColumnSequence(){
	return this.columnSequence.toString();
    }
    
    public String getFullSequence(){
	return this.fullSequence.toString();
    }

    public String getSequenceFromBases(){
	StringBuffer bf = new StringBuffer();
	for(int i=0;i<this.boundaries.length;i++){
	    int curStart = this.boundaries[i]-1;
	    int curE = -1;
	    if(i<(this.boundaries.length-1))
		curE = this.boundaries[i+1]-1;
	    else
		curE = this.seq.size();
	    for(int j=curStart; j<curE; j++){
		bf.append(this.seq.get(j).getBase());
	    }
	    //bf.append("|\n");
	}
	return bf.toString();
    }

    public int[] getSegmentOffsets(){
	return this.segmentOffsets;
    }
    
    public int[] getCumulativeOffsets(){
	return this.cumulativeOffsets;
    }

    public int[] getBoundaries(){
	return this.boundaries;
    }
    
    public String getNthBlockColumnSequence(int n){
	//if it's last block
	if(n == (this.boundaries.length - 1))
	    return this.columnSequence.substring(this.boundaries[n]-1);
	else
	    return this.columnSequence.substring(this.boundaries[n]-1, this.boundaries[n+1]-1);
    }

    //returns nth intron. here n is 0-based
    //   n(0-based)  intronNum   boundariesIndex
    //       0          1              0         
    //       1          2              2
    //       2          3              4
    public ArrayList<Base> getNthIntron(int n){
	int index = n * 2; // from n to boundariesIndex
	int sIndex = this.boundaries[index]-1;
	int eIndex = -1;
	if(index == this.boundaries.length-1)//last intron
	    eIndex = this.seq.size();
	else
	    eIndex = this.boundaries[index+1]-1;
	ArrayList<Base> tmp = new ArrayList<Base>();
	
	for(int i=sIndex; i<eIndex; i++){
	    tmp.add(this.seq.get(i));
	}
	
	return tmp;
	
	//return (ArrayList<Base>) this.seq.subList(sIndex, eIndex);
    }

    public int numBlocks(){
	return this.boundaries.length;
    }

    /* this processes nuc only allele --> only containing EXONS*/
    public Sequence(String allele, String msfSequence, Sequence ref){
	this();
	//HLA.log.appendln(">>>>>> PROCESSING\t" + allele + "\t<<<<<<");
	this.alleleName = allele;
	String[] tokens = msfSequence.split("\\|");

	/* we set the number of blocks same as the reference sequence */
	this.boundaries = new int[ref.numBlocks()];
	this.segmentOffsets = new int[ref.numBlocks()];
	this.cumulativeOffsets = new int[ref.numBlocks()];

	/* first we copy the first intron from the reference */
	this.seq.addAll(ref.getNthIntron(0));
	this.boundaries[0] = ref.getBoundaries()[0];
	this.segmentOffsets[0] = ref.getSegmentOffsets()[0];
	this.cumulativeOffsets[0] = ref.getCumulativeOffsets()[0];
	this.columnSequence.append(ref.getColumnSequence().substring(0, ref.getBoundaries()[1]-1));
	this.fullSequence.append(ref.getFullSequence().substring(0,ref.getBoundaries()[1]-1-ref.getCumulativeOffsets()[0]));
	/*
	  HLA.log.appendln("COPYING FIRST INTRON:");
	  HLA.log.appendln("BS:" + ref.getNthIntron(0).size());
	  HLA.log.appendln("CS:" + ref.getColumnSequence().substring(0, ref.getBoundaries()[1]-1));
	  HLA.log.appendln("FS:" + ref.getFullSequence().substring(0,ref.getBoundaries()[1]-1-ref.getCumulativeOffsets()[0]));
	*/
	//set offset/curStartColPos accordingly
	int offset = ref.getCumulativeOffsets()[0];
	int curStartColPos = ref.getBoundaries()[1];
	/*
	if(this.seq.size() != curStartColPos-1){
	    
	    HLA.log.appendln("SOMETHING WRONG: seq.size["+ this.seq.size()+"] and colPosViaBoundaries[" +curStartColPos+ "] doesn't match" );
	    }*/

	boolean isExon = true;
	int exonNum = 0;
	/* foreach exon*/
	for(int i=0; i<tokens.length; i++){
	    /* process current exon index: 2*i+1 */
	    exonNum++;
	    //exon sequence is still in abbrv form with '-' matching base for ref.
	    //need to modify the sequence
	    /*
	    HLA.log.appendln(tokens[i]);
	    HLA.log.appendln(ref.getNthBlockColumnSequence(2*i+1));
	    */
	    String modseq = MergeMSFs.abbrv2Seq(tokens[i], ref.getNthBlockColumnSequence(2*i+1));
	    /*
	      HLA.log.appendln(this.columnSequence + " (CS)");
	      HLA.log.appendln(this.fullSequence + " (FS)");
	      HLA.log.appendln("COPYING EXON " + (i+1) + ":");
	      HLA.log.appendln(modseq);
	    */
	    //cumulative offset
	    int updatedOffset = processBlock(modseq, isExon, exonNum, offset);
	    /*
	      HLA.log.appendln("AFTER COPYING");
	      HLA.log.appendln(this.columnSequence + " (CS)");
	      HLA.log.appendln(this.fullSequence + " (FS)");
	    */
	    //current block offset
	    this.segmentOffsets[2*i+1] = updatedOffset - offset;
	    this.cumulativeOffsets[2*i+1] = updatedOffset;
	    //update offset with cumOffset
	    offset = updatedOffset;
	    this.boundaries[2*i+1] = curStartColPos;
	    
	    int offsetDifference = offset - ref.getCumulativeOffsets()[2*i+1]; // need to calculate the offesetDifference to shift.
	    
	    /* process next intron index: 2*(i+1)*/
	    //when adding bases, we need to use the updated offset. //04/14/16
	    if(offsetDifference == 0)
		this.seq.addAll(ref.getNthIntron(i+1));
	    else
		this.addAllWithOffset(ref.getNthIntron(i+1), offsetDifference);
	    
	    this.boundaries[2*(i+1)] = ref.getBoundaries()[2*(i+1)];
	    this.segmentOffsets[2*(i+1)] = ref.getSegmentOffsets()[2*(i+1)];
	    offset = offset + ref.segmentOffsets[2*(i+1)];
	    this.cumulativeOffsets[2*(i+1)] = offset;
	    if(i < (tokens.length-1)){
		//HLA.log.appendln("intron ("+(2*(i+1))+")\tCI(" + (ref.getBoundaries()[2*(i+1)]-1) + "," + (ref.getBoundaries()[2*(i+1)+1]-1) +")");
		//HLA.log.appendln(ref.getColumnSequence().substring(ref.getBoundaries()[2*(i+1)]-1, ref.getBoundaries()[2*(i+1)+1]-1));
		this.columnSequence.append(ref.getColumnSequence().substring(ref.getBoundaries()[2*(i+1)]-1, ref.getBoundaries()[2*(i+1)+1]-1));
		//HLA.log.appendln("intron ("+(2*(i+1))+")\tFI(" + (ref.getBoundaries()[2*(i+1)]-1-ref.getCumulativeOffsets()[2*(i+1)-1]) + "," 
		//		   + (ref.getBoundaries()[2*(i+1)+1]-1-ref.getCumulativeOffsets()[2*(i+1)]) + ")");
		//HLA.log.appendln(ref.getFullSequence().substring(ref.getBoundaries()[2*(i+1)]-1-ref.getCumulativeOffsets()[2*(i+1)-1]
		//						   , ref.getBoundaries()[2*(i+1)+1]-1-ref.getCumulativeOffsets()[2*(i+1)]));
		this.fullSequence.append(ref.getFullSequence().substring(ref.getBoundaries()[2*(i+1)]-1-ref.getCumulativeOffsets()[2*(i+1)-1]
									 , ref.getBoundaries()[2*(i+1)+1]-1-ref.getCumulativeOffsets()[2*(i+1)]));
		curStartColPos = ref.getBoundaries()[2*(i+1)+1]; //need to fetch next 
		if((curStartColPos-1) != this.seq.size())
		    HLA.log.appendln("[IF]SOMETHING WRONG:\t SIZES DONT MATCH!!");
	    }else{//if it's last exon
		//HLA.log.appendln("intron ("+(2*(i+1))+")\tCI(" + (ref.getBoundaries()[2*(i+1)]-1) + ",END" );
		//HLA.log.appendln(ref.getColumnSequence().substring(ref.getBoundaries()[2*(i+1)]-1));
		this.columnSequence.append(ref.getColumnSequence().substring(ref.getBoundaries()[2*(i+1)]-1));
		//HLA.log.appendln("intron ("+(2*(i+1))+")\tFI(" + (ref.getBoundaries()[2*(i+1)]-1-ref.getCumulativeOffsets()[2*(i+1)-1]) + ",END" );
		//HLA.log.appendln(ref.getFullSequence().substring(ref.getBoundaries()[2*(i+1)]-1-ref.getCumulativeOffsets()[2*(i+1)-1]));
		this.fullSequence.append(ref.getFullSequence().substring(ref.getBoundaries()[2*(i+1)]-1-ref.getCumulativeOffsets()[2*(i+1)-1]));
	    }
	}
    }

    private void addAllWithOffset(ArrayList<Base> intronBases, int offsetDifference){
	for(int i=0; i<intronBases.size(); i++)
	    this.seq.add(intronBases.get(i).deepCopyWithOffset(offsetDifference));
    }

    
    //msfSequence still has intron/exon boundary symbol Embedded.
    // <INTRON1>|<EXON1>|<INTRON2>|<EXON2>|...
    // allele --> allelename
    // msfSequence --> msf sequence string without blanks
    public Sequence(String allele, String msfSequence){
	this(allele, msfSequence, false, null);
    }
    public Sequence(String allele, String msfSequence, boolean replaceAbbrv, Sequence ref){
	this();
	this.alleleName = allele;
	String[] tokens = msfSequence.split("\\|");
	this.boundaries = new int[tokens.length];
	this.segmentOffsets = new int[tokens.length];
	this.cumulativeOffsets = new int[tokens.length];
	int offset = 0;
	boolean isExon = false;
	int intronNum = 0;
	int exonNum = 0;
	int curStartColPos = 0;
	//for introns and exons
	for(int i=0; i<tokens.length; i++){
	    String blockSeq = null;
	    if(replaceAbbrv)
		blockSeq = MergeMSFs.abbrv2Seq(tokens[i], ref.getNthBlockColumnSequence(i));
	    else
		blockSeq = tokens[i];
	    curStartColPos++;
	    this.boundaries[i] = curStartColPos;
	    if(i%2 == 0){//intron 0, 2, 4, 6
		isExon = false;
		intronNum++;
		int updatedOffset = processBlock(blockSeq, isExon, intronNum, offset);
		this.segmentOffsets[i] = updatedOffset - offset;
		this.cumulativeOffsets[i] = updatedOffset;
		offset = updatedOffset;
	    }else{//exon 1, 3, 5, 7
		isExon = true;
		exonNum++;
		int updatedOffset = processBlock(blockSeq, isExon, exonNum, offset);
		this.segmentOffsets[i] = updatedOffset - offset;
		this.cumulativeOffsets[i] = updatedOffset;
		offset = updatedOffset;
	    }
	    curStartColPos = this.seq.size();
	}
    }

    public static String replaceUnknownAsGap(String str){
	StringBuffer bf = new StringBuffer();
	for(int i=0; i<str.length(); i++){
	    if(str.charAt(i) == '*')
		bf.append('.');
	    else
		bf.append(str.charAt(i));
	}
	return bf.toString();
    }

    
    //given msf formatted(no blanks) sequence and add bases 
    //returns the base2coloffset.
    public int processBlock(String blkSeq, boolean isExon, int intronExonNum, int offset){
	String blockSeq = Sequence.replaceUnknownAsGap(blkSeq);
	int colPos = this.seq.size(); //1-based column Position --> this is the last position of previous block
	int base2colOffset = offset; 
	int basePos = colPos - base2colOffset; //also last position of previous block
	
	/*
	if(this.seq.size() > 0){
	    if(colPos == this.seq.get(this.seq.size()-1).getColPos() && basePos == this.seq.get(this.seq.size()-1).getBasePos()){
		;	    
	    }else{
		HLA.log.appendln("Doesn't Match\t[inferredCP:" + colPos + "][CP:" + this.seq.get(this.seq.size()-1).getColPos()+"]\t[inferredBP:" + basePos + "][BP:" + this.seq.get(this.seq.size()-1).getBasePos()+"]");
	    }
	}
	*/
	
	for(int i=0; i<blockSeq.length(); i++){
	    char curBase = blockSeq.charAt(i);
	    this.columnSequence.append(curBase);
	    colPos++;
	    if(Base.isBase(curBase)){
		basePos++;
		this.fullSequence.append(curBase);
	    }else if(Base.isGap(curBase))
		base2colOffset++;
	    else{
		HLA.log.appendln("WHAT ELSE????\nBlockSeq:" + blockSeq + "\n@"  + (i+1) + ":" + curBase);
		try{
		    throw new Exception("ERRR");
		}catch(Exception e){
		    e.printStackTrace();
		}
		    
	    }
		    
	    this.seq.add(new Base(blockSeq.charAt(i), basePos, colPos, base2colOffset, isExon, intronExonNum));
	}
	
	return base2colOffset;
    }
    
}
