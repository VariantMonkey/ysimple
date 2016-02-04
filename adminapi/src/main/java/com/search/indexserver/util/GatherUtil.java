package com.search.indexserver.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.search.indexserver.pojo.DocIdAndRank;
import com.search.indexserver.pojo.TermCodeAndTermInfoList;
import com.search.indexserver.pojo.TermInOneDoc;
import com.search.indexserver.pojo.TermIntersection;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by yjj on 15/11/22.
 * 集合工具类，集合求交
 */
public class GatherUtil {

    private static List<TermIntersection> intersectionByBinSearch(int leftCode, List<TermInOneDoc> left, int rightCode, List<TermInOneDoc> right) {

        List<TermIntersection> res = Lists.newArrayList();
        int min = 0;
        int max = right.size();
        for (TermInOneDoc termInOneDoc : left) {
            int index = Collections.binarySearch(right.subList(min, max), termInOneDoc.getDocId());
            if (index >= 0) {

                Map<Integer, TermInOneDoc> termInfoMap = Maps.newHashMap();
                termInfoMap.put(leftCode, termInOneDoc);
                termInfoMap.put(rightCode, right.get(index));
                res.add(new TermIntersection(termInOneDoc.getDocId(), termInfoMap));
                min = index;
            }
        }

        return res;
    }

    private static List<TermIntersection> intersectionOneByOne(int leftCode, List<TermInOneDoc> left, int rightCode, List<TermInOneDoc> right) {

        List<TermIntersection> res = Lists.newArrayList();

        int p1 = 0, p2 = 0;
        while (Ints.compare(p1, left.size()) == -1 && Ints.compare(p2, right.size()) == -1) {
            int compare = Ints.compare(left.get(p1).getDocId(), right.get(p2).getDocId());
            if (compare == 0) {
                Map<Integer, TermInOneDoc> termInfoMap = Maps.newHashMap();
                termInfoMap.put(leftCode, left.get(p1));
                termInfoMap.put(rightCode, right.get(p2));
                res.add(new TermIntersection(left.get(p1).getDocId(), termInfoMap));
                p1++;
                p2++;
            } else if (compare < 0) {
                p1++;
            } else {
                p2++;
            }
        }


        return res;
    }

    private static List<TermIntersection> intersectionOnlyTwo(List<TermCodeAndTermInfoList> termCodeAndTermInfoList) {

        int diff = termCodeAndTermInfoList.get(1).getTermInOneDocList().size() / termCodeAndTermInfoList.get(0).getTermInOneDocList().size();

        if (diff > 20 || (1 << (diff + 1) > termCodeAndTermInfoList.get(1).getTermInOneDocList().size())) {
            return intersectionByBinSearch(termCodeAndTermInfoList.get(0).getTermCode(), termCodeAndTermInfoList.get(0).getTermInOneDocList(),
                    termCodeAndTermInfoList.get(1).getTermCode(), termCodeAndTermInfoList.get(1).getTermInOneDocList());
        }

        return intersectionOneByOne(termCodeAndTermInfoList.get(0).getTermCode(), termCodeAndTermInfoList.get(0).getTermInOneDocList(),
                termCodeAndTermInfoList.get(1).getTermCode(), termCodeAndTermInfoList.get(1).getTermInOneDocList());

    }

    private static List<TermIntersection> intersectionOnlyOne(List<TermCodeAndTermInfoList> termCodeAndTermInfoList) {

        List<TermIntersection> res = Lists.newArrayList();
        Integer termCode = termCodeAndTermInfoList.get(0).getTermCode();
        for (TermInOneDoc termInOneDoc : termCodeAndTermInfoList.get(0).getTermInOneDocList()) {

            Map<Integer, TermInOneDoc> termInfoMap = Maps.newHashMap();
            termInfoMap.put(termCode, termInOneDoc);
            res.add(new TermIntersection(termInOneDoc.getDocId(), termInfoMap));
        }

        return res;
    }


    private static List<TermIntersection> intersectionByBinSearch(List<TermIntersection> termIntersectionList, int rightCode, List<TermInOneDoc> right) {

        List<TermIntersection> res = Lists.newArrayList();


        int min = 0;
        int max = right.size();
        for (TermIntersection termIntersection : termIntersectionList) {

            int index = Collections.binarySearch(right.subList(min, max), termIntersection.getDocId());
            if (index > 0) {
                Map<Integer, TermInOneDoc> termInfoMap = termIntersection.getTermInfoMap();
                termInfoMap.put(rightCode, right.get(index));
                res.add(new TermIntersection(termIntersection.getDocId(), termInfoMap));
                min = index;
            }

        }
        return res;

    }

    private static List<TermIntersection> intersectionOneByOne(List<TermIntersection> termIntersectionList, int rightCode, List<TermInOneDoc> right) {

        List<TermIntersection> res = Lists.newArrayList();

        int p1 = 0, p2 = 0;
        while (Ints.compare(p1, termIntersectionList.size()) == -1 && Ints.compare(p2, right.size()) == -1) {
            int compare = Ints.compare(termIntersectionList.get(p1).getDocId(), right.get(p2).getDocId());
            if (compare == 0) {
                Map<Integer, TermInOneDoc> termInfoMap = termIntersectionList.get(p1).getTermInfoMap();
                termInfoMap.put(rightCode, right.get(p2));
                res.add(new TermIntersection(right.get(p1).getDocId(), termInfoMap));
                p1++;
                p2++;
            } else if (compare < 0) {
                p1++;
            } else {
                p2++;
            }
        }

        return res;

    }

    private static List<TermIntersection> intersection(List<TermIntersection> termIntersectionList, int rightCode, List<TermInOneDoc> right) {

        int diff = right.size() / termIntersectionList.size();

        if (diff > 20 || (1 << (diff + 1) > right.size())) {
            return intersectionByBinSearch(termIntersectionList, rightCode, right);
        }

        return intersectionOneByOne(termIntersectionList, rightCode, right);
    }


    public static List<TermIntersection> intersectionByBinSearch(List<TermCodeAndTermInfoList> termCodeAndTermInfoList) {

        if (CollectionUtils.isEmpty(termCodeAndTermInfoList)) {
            return Lists.newArrayList();
        }

        Collections.sort(termCodeAndTermInfoList, new Comparator<TermCodeAndTermInfoList>() {
            public int compare(TermCodeAndTermInfoList o1, TermCodeAndTermInfoList o2) {
                return Ints.compare(o1.getTermInOneDocList().size(), o2.getTermInOneDocList().size());
            }
        });

        List<TermIntersection> res;
        if (termCodeAndTermInfoList.size() == 1) {
            res = intersectionOnlyOne(termCodeAndTermInfoList);
            return res;

        }

        res = intersectionOnlyTwo(termCodeAndTermInfoList);
        for (int i = 2; i < termCodeAndTermInfoList.size(); i++) {
            res = intersection(res, termCodeAndTermInfoList.get(i).getTermCode(), termCodeAndTermInfoList.get(i).getTermInOneDocList());
        }

        return res;
    }


    public static void topN(List<DocIdAndRank> docIdAndRankResultList, int docId, double rank, int topN) {

        if (docIdAndRankResultList.size() == 0) {
            docIdAndRankResultList.add(new DocIdAndRank(docId, rank));
            return;
        }

        int flag = Ints.compare(docIdAndRankResultList.size(), topN);
        if (flag == 0 && Double.compare(docIdAndRankResultList.get(0).getRank(), rank) > 0) {
            return;
        }

        int index = Collections.binarySearch(docIdAndRankResultList, rank);
        index = index < 0 ? Math.abs(index + 1) : index;
        docIdAndRankResultList.add(index, new DocIdAndRank(docId, rank));

        if (docIdAndRankResultList.size() > topN) {
            docIdAndRankResultList.remove(0);
        }

    }


    public static void topN(List<DocIdAndRank> docIdAndRankResultList, DocIdAndRank docIdAndRank, int topN) {

        if (docIdAndRankResultList.size() == 0) {
            docIdAndRankResultList.add(docIdAndRank);
            return;
        }

        int flag = Ints.compare(docIdAndRankResultList.size(), topN);
        if (flag == 0 && Double.compare(docIdAndRankResultList.get(0).getRank(), docIdAndRank.getRank()) > 0) {
            return;
        }

        int index = Collections.binarySearch(docIdAndRankResultList, docIdAndRank.getRank());
        index = index < 0 ? Math.abs(index + 1) : index;
        docIdAndRankResultList.add(index, docIdAndRank);

        if (docIdAndRankResultList.size() > topN) {
            docIdAndRankResultList.remove(0);
        }

    }



}
