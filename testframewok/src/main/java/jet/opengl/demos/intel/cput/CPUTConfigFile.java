package jet.opengl.demos.intel.cput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/11/13.
 */
public class CPUTConfigFile {
    private CPUTConfigBlock    []mpBlocks;

    public void LoadFile(String szFilename) throws IOException{
        List<CPUTConfigBlock> blocks = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.open(szFilename)))){
            String line;

            CPUTConfigBlock currentBlock = null;
            while ((line = in.readLine()) != null){
                // Remove comment
                int commentPos = line.indexOf("//");
                if(commentPos >=0){
                    line = line.substring(0, commentPos);
                }

                line = line.trim();
                if(line.isEmpty()) continue;
                int nOpenBracketIndex = line.indexOf('[');
                int nCloseBracketIndex = line.lastIndexOf(']');
                if(nOpenBracketIndex != -1 && nCloseBracketIndex != -1){
                    if(currentBlock != null)
                        blocks.add(currentBlock);

                    currentBlock = new CPUTConfigBlock();
                    currentBlock.mszName = line.substring(nOpenBracketIndex + 1, nCloseBracketIndex).toLowerCase();
                }else {
                    // It's a value
                    if(currentBlock == null){
                        currentBlock = new CPUTConfigBlock();
                    }

                    int nEqualsIndex = line.indexOf('=');
                    if(nEqualsIndex == -1)
                    {
                        boolean dup = false;
                        // No value, just a key, save it anyway
                        for(int ii=0;ii<currentBlock.mnValueCount;++ii)
                        {
                            if(!currentBlock.mpValues[ii].szName.equals(line))
                            {
                                dup = true;
                                break;
                            }
                        }
                        if(!dup)
                        {
                            /*pCurrBlock->mpValues[pCurrBlock->mnValueCount].szName = szCurrLine;
                            pCurrBlock->mnValueCount++;*/
                            currentBlock.mpValues[currentBlock.mnValueCount++] = new CPUTConfigEntry(line, "");
                        }
                    }
                    else
                    {
                        /*cString szValue = szCurrLine.substr(nEqualsIndex+1);
                        cString szName = szCurrLine.erase(nEqualsIndex, 1024);
                        RemoveWhitespace(szValue);
                        RemoveWhitespace(szName);
                        std::transform(szName.begin(), szName.end(), szName.begin(), ::tolower);*/
                        String szName = line.substring(0, nEqualsIndex).trim().toLowerCase();
                        String szValue = line.substring(nEqualsIndex+1).trim().toLowerCase();

                        boolean dup = false;
                        for(int ii=0;ii<currentBlock.mnValueCount;++ii)
                        {
                            if(currentBlock.mpValues[ii].szName.equals(szName))
                            {
                                dup = true;
                                break;
                            }
                        }
                        if(!dup)
                        {
                            /*pCurrBlock->mpValues[pCurrBlock->mnValueCount].szValue = szValue;
                            pCurrBlock->mpValues[pCurrBlock->mnValueCount].szName = szName;
                            pCurrBlock->mnValueCount++;*/
                            currentBlock.mpValues[currentBlock.mnValueCount++] = new CPUTConfigEntry(szName, szValue);
                        }
                    }
                }
            }

            if(currentBlock != null){
                blocks.add(currentBlock);
            }
        }

        mpBlocks = blocks.toArray(new CPUTConfigBlock[blocks.size()]);
    }

    public CPUTConfigBlock GetBlock(int nBlockIndex){
        if(nBlockIndex >= mpBlocks.length || nBlockIndex < 0)
        {
            return null;
        }
        return mpBlocks[nBlockIndex];
    }

    public CPUTConfigBlock GetBlockByName(String blockName){
        for(int ii=0; ii<mpBlocks.length; ++ii)
        {
            if(mpBlocks[ii].mszName.equalsIgnoreCase(blockName))
            {
                return mpBlocks[ii];
            }
        }
        return null;
    }

    public int BlockCount(){
        return mpBlocks.length;
    }
}
