// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.repl;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.relational.autocomplete.RelationalCompleterExtension;
import org.junit.Assert;
import org.junit.Test;

public class TestCompleter
{
    @Test
    public void testRelationAccessor()
    {
        Assert.assertEquals("[a::A , >{a::A.]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>")));
        Assert.assertEquals("[a::other , >{a::other.], [a::ABC , >{a::ABC.]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a")));
        Assert.assertEquals("[a::ABC , >{a::ABC.]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A")));
        Assert.assertEquals("[a::ABC , >{a::ABC.]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC")));
        Assert.assertEquals("[t , t}#]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Table t(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC.")));
        Assert.assertEquals("[at , at}#], [t , t}#], [ts , ts.]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Schema ts(Table t(col VARCHAR(200))) Table t(col VARCHAR(200)) Table at(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC.")));
        Assert.assertEquals("[t , t}#], [ts , ts.]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Schema ts(Table t(col VARCHAR(200))) Table t(col VARCHAR(200)) Table at(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC.t")));
        Assert.assertEquals("[ts , ts.]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Schema ts(Table t(col VARCHAR(200))) Table t(col VARCHAR(200)) Table at(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC.ts")));
        Assert.assertEquals("[t , t}#]", checkResultNoException(new Completer("###Relational\nDatabase a::ABC(Schema ts(Table t(col VARCHAR(200))) Table t(col VARCHAR(200)) Table at(col VARCHAR(200)))\nDatabase a::other(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::ABC.ts.")));
        Assert.assertEquals("[tab , tab}#]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table tab(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.x")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table co(val INTEGER) Table tab(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.tab}#")));
    }

    @Test
    public void testAutocompleteFunctionParameter()
    {
        Assert.assertEquals("[test::test , test::test)]", checkResultNoException(new Completer(db + connection + runtime, Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{test::TestDatabase.tb}#->from(")));
        Assert.assertEquals("[test::test , test::test)]", checkResultNoException(new Completer(db + connection + runtime, Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{test::TestDatabase.tb}#->from(te")));
        Assert.assertEquals("", checkResultNoException(new Completer(db + connection + runtime, Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{test::TestDatabase.tb}#->from(zte")));
    }

    @Test
    public void testArrowOnFunction()
    {
        Assert.assertEquals("[cast , cast(], [distinct , distinct(], [drop , drop(], [select , select(], [extend , extend(], [filter , filter(], [from , from(], [groupBy , groupBy(], [pivot , pivot(], [join , join(], [asOfJoin , asOfJoin(], [limit , limit(], [rename , rename(], [size , size(], [slice , slice(], [sort , sort(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|$x.col == 'oo')->")));
        Assert.assertEquals("PARSER error at [6:1-23]: parsing error", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->limit(10)-").getEngineException().toPretty());
    }

    @Test
    public void testArrowRelation()
    {
        Assert.assertEquals("[cast , cast(], [distinct , distinct(], [drop , drop(], [select , select(], [extend , extend(], [filter , filter(], [from , from(], [groupBy , groupBy(], [pivot , pivot(], [join , join(], [asOfJoin , asOfJoin(], [limit , limit(], [rename , rename(], [size , size(], [slice , slice(], [sort , sort(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->")));
        Assert.assertEquals("[select , select(], [size , size(], [slice , slice(], [sort , sort(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->s")));
    }

    @Test
    public void testArrowDeep()
    {
        Assert.assertEquals("[contains , contains(], [startsWith , startsWith(], [endsWith , endsWith(], [toLower , toLower(], [toUpper , toUpper(], [lpad , lpad(], [rpad , rpad(], [parseInteger , parseInteger(], [parseFloat , parseFloat(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(f|$f.col->")));
    }

    @Test
    public void testErrors()
    {
        Assert.assertEquals("PARSER error at [5:21]: Unexpected token", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(*").getEngineException().toPretty());
        Assert.assertEquals("PARSER error at [6:1-21]: parsing error", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(!").getEngineException().toPretty());
        Assert.assertEquals("COMPILATION error at [5:1-12]: The store 'a::As' can't be found.", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::As.t}#->select(~").getEngineException().toPretty());
    }

    @Test
    public void testDeepWithCompilationError()
    {
        Assert.assertEquals("COMPILATION error at [5:26-49]: Can't find a match for function 'plus(Any[2])'.\n" +
                "Functions that can match if parameter types or multiplicities are changed:\n" +
                "\t\tplus(String[*]):String[1]\n" +
                "\t\tplus(Integer[*]):Integer[1]\n" +
                "\t\tplus(Float[*]):Float[1]\n" +
                "\t\tplus(Decimal[*]):Decimal[1]\n" +
                "\t\tplus(Number[*]):Number[1]\n", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'p'+$x.col->startsWith('x'))->fr").getEngineException().toPretty());
        Assert.assertEquals("COMPILATION error at [5:24]: Can't find type 'x'", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(~x:x.").getEngineException().toPretty());
    }

    @Test
    public void testArrowPostCol()
    {
        Assert.assertEquals("[ascending , ascending(], [descending , descending(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->sort(~col->")));
    }

    @Test
    public void testMultiLevelFunction()
    {
        Assert.assertEquals("[select , select(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~[col])->join(#>{a::A.t}#->selec")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~[col])->join(#>{a::A.t}#->select(~")));
    }

    //--------
    // Filter
    //--------
    @Test
    public void testDotInFilterDeepRelation()
    {
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.co")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|'x'+[1,2]->map(z|$z+$x.z")));
        Assert.assertEquals("[na col , 'na col']", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(\"na col\" VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->filter(x|$x.'na")));
    }

    //--------
    // Rename
    //--------
    @Test
    public void testRenameFirstParam()
    {
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~co")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~x")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~col,~")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->rename(~")));
    }

    //--------
    // Extend
    //--------
    @Test
    public void testDotInExtend()
    {
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(~x:y|$y.")));
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(~[x:y|$y.")));
        Assert.assertEquals("[count , count(], [joinStrings , joinStrings(], [uniqueValueOnly , uniqueValueOnly(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(~[x:y|$y.col:z|$z->")));
    }

    @Test
    public void testExtendWithOver()
    {
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(over(~")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(over(~[")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(over([~")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(over(~col->ascending()), ~nc:{p,f,r|$r.")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(over(~col), ~nc:{p,f,r|$r.")));
        Assert.assertEquals("[count , count(], [joinStrings , joinStrings(], [uniqueValueOnly , uniqueValueOnly(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(over(~col->ascending()), ~nc:{p,f,r|$r.col}:y|$y->")));
    }

    @Test
    public void testExtendWithOverError()
    {
        Assert.assertEquals("COMPILATION error at [5:55-56]: Can't find variable class for variable 'p' in the graph", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->extend(over(~Name->descending()), {p,f,r|$p->lead($r).").getEngineException().toPretty());
    }

    //---------
    // GroupBy
    //---------
    @Test
    public void testGroupBy()
    {
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~[")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~[col,")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~col, ~z:x|$x.")));
        Assert.assertEquals("[val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~col, ~[z:x|$x.v")));
        Assert.assertEquals("[sum , sum(], [mean , mean(], [average , average(], [min , min(], [max , max(], [count , count(], [percentile , percentile(], [variancePopulation , variancePopulation(], [varianceSample , varianceSample(], [stdDevPopulation , stdDevPopulation(], [stdDevSample , stdDevSample(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->groupBy(~col, ~[z:x|$x.val:y|$y->")));
    }

    //---------
    // Pivot
    //---------
    @Test
    public void testPivot()
    {
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->pivot(~")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->pivot(~[")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->pivot(~[col,")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->pivot(~col, ~z:x|$x.")));
        Assert.assertEquals("[val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->pivot(~col, ~[z:x|$x.v")));
        Assert.assertEquals("[sum , sum(], [mean , mean(], [average , average(], [min , min(], [max , max(], [count , count(], [percentile , percentile(], [variancePopulation , variancePopulation(], [varianceSample , varianceSample(], [stdDevPopulation , stdDevPopulation(], [stdDevSample , stdDevSample(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->pivot(~col, ~[z:x|$x.val:y|$y->")));
    }

    //---------
    // Cast (Relation)
    //---------
    @Test
    public void testRelationCast()
    {
        Assert.assertEquals("[@meta::pure::metamodel::relation::Relation<( , @meta::pure::metamodel::relation::Relation<(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200)))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->cast(")));
    }

    //------
    // Join
    //------
    @Test
    public void testJoin()
    {
        Assert.assertEquals("[a::A , >{a::A.]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>")));
        Assert.assertEquals("[t , t}#]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.")));
        Assert.assertEquals("[JoinKind , JoinKind.]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, ")));
        Assert.assertEquals("[LEFT , LEFT], [INNER , INNER]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER, {a,b|$a.")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER,")));
        Assert.assertEquals("[k , k], [o , o]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t2}#, JoinKind.INNER, {a,b|$a.col == $b.")));
        Assert.assertEquals("COMPILATION error at [5:14-17]: \"The relation contains duplicates: [val, col]\"", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->join(#>{a::A.t}#, JoinKind.INNER, {a,b|$a.val == $b.val})->").getEngineException().toPretty());
    }

    //------
    // AsOfJoin
    //------
    @Test
    public void testAsOfJoin()
    {
        Assert.assertEquals("[a::A , >{a::A.]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>")));
        Assert.assertEquals("[t , t}#]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t}#, {a,b|$a.")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t}#,")));
        Assert.assertEquals("[k , k], [o , o]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t2}#, {a,b|$a.col == $b.")));
        Assert.assertEquals("", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t2}#, {a,b|$a.col == $b.k}, ")));
        Assert.assertEquals("[k , k], [o , o]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t2}#, {a,b|$a.col == $b.k}, {b,c|$c.")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t2}#, {a,b|$a.col == $b.k}, {b,c|$b.")));
        Assert.assertEquals("[filter , filter(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t2(k VARCHAR(200), o INT) Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t2}#, {a,b|$a.col == $b.k}, {b,c|$c.k == $b.col})->fil")));
        Assert.assertEquals("COMPILATION error at [5:14-21]: \"The relation contains duplicates: [val, col]\"", new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->asOfJoin(#>{a::A.t}#, {a,b|$a.val == $b.val})->").getEngineException().toPretty());
    }

    //--------
    // Select
    //--------
    @Test
    public void testSelect()
    {
        Assert.assertEquals("[col , col]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~c")));
        Assert.assertEquals("[col , col], [val , val]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(col VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~[col,")));
        Assert.assertEquals("[from , from(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(\"col space\" VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~'col space')->fro")));
        Assert.assertEquals("[from , from(]", checkResultNoException(new Completer("###Relational\nDatabase a::A(Table t(\"col space\" VARCHAR(200), val INT))", Lists.mutable.with(new RelationalCompleterExtension())).complete("#>{a::A.t}#->select(~['col space'])->fro")));
    }


    private static String db = "###Relational\n" +
            "Database test::TestDatabase(Table tb(col VARCHAR(200)))\n";

    private static String connection = "###Connection\n" +
            "RelationalDatabaseConnection test::testConnection\n" +
            "{\n" +
            "   store: test::TestDatabase;" +
            "   specification: LocalH2{};" +
            "   type: H2;" +
            "   auth: DefaultH2;\n" +
            "}\n";

    private static String runtime = "###Runtime\n" +
            "Runtime test::test\n" +
            "{\n" +
            "   mappings : [];\n" +
            "   connections:\n" +
            "   [\n" +
            "       test::TestDatabase : [connection: test::testConnection]\n" +
            "   ];\n" +
            "}\n";

    private String checkResultNoException(CompletionResult completion)
    {
        Assert.assertNull(completion.getEngineException());
        return completion.getCompletion().makeString(", ");
    }
}
