package jet.parsing.cplus.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/7/22 0022.
 */

public class CExpressionGroup {
    private final List<CExpression> children = new ArrayList<>();

    public void add(CExpression expression){
        children.add(expression);
    }
}
