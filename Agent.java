import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;

import genius.core.actions.Action;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.parties.NegotiationInfo;
import genius.core.actions.Offer;
import genius.core.actions.Accept;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.timeline.*;
import genius.core.issue.*;
import genius.core.utility.*;

class opModel {
    private HashMap<Issue, HashMap<Value , Double>> frequencyHashMap = new HashMap<Issue, HashMap<Value , Double>>();
    private double totalValuesCount = 0;
    private TimeLineInfo timeline;

    public opModel(UtilitySpace utilitySpace, TimeLineInfo timeline) {
        this.timeline = timeline;
        for (Issue issue : utilitySpace.getDomain().getIssues()) {
            List<ValueDiscrete> values = ((IssueDiscrete)issue).getValues();
            HashMap<Value, Double> valueHashMap = new HashMap<Value, Double>();
            for (Value value : values) {
                valueHashMap.put(value, 0.0);
            }
            frequencyHashMap.put(issue, valueHashMap);
        }
    }

    public void updateModel(Bid bid) {
        for (int issueIndex = 0; issueIndex < bid.getIssues().size(); issueIndex++) {
            double weight = Math.pow(timeline.getCurrentTime()/timeline.getTotalTime(), Math.E);
            Issue issue = bid.getIssues().get(issueIndex);
            Value issueValue = findValue(bid, issueIndex);
            HashMap<Value, Double> valueHashMap = frequencyHashMap.get(issue);
            double tbi = valueHashMap.get(issueValue) + weight;
            valueHashMap.put(issueValue, tbi);
            totalValuesCount = totalValuesCount + weight;
        }
    }

    public double getUtility(Bid bid) {
        double utility = 0;
        for (int issueIndex = 0; issueIndex < bid.getIssues().size(); issueIndex++) {
            Issue issue = bid.getIssues().get(issueIndex);
            Value issueValue = findValue(bid, issueIndex);

            HashMap<Value, Double> valueHashMap = frequencyHashMap.get(issue);
            double weight = valueHashMap.get(issueValue);
            weight /= totalValuesCount;
            utility += weight;
        }
        return utility;
    }

    private Value findValue(Bid bid, int issueIndex) {
        Value issueValue = null;
        try {
            issueValue = bid.getValue(issueIndex+1);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return issueValue;
    }
}

public class Agent extends AbstractNegotiationParty {

    private Bid lastReceivedBid = null;
    private opModel opponentModel = null;

    private double thresholdBidUtility = 0.85; // bids above this value get accepted
    private double thresholdTime = 0.98; // if negotiation time is above this value, bid is accepted
    private ArrayList<Bid> bidList;

    Comparator<Bid> bidComparator = new Comparator<Bid>() {
        @Override
        public int compare(Bid a, Bid b) {
            return Double.compare(getUtility(b), getUtility(a));
        }
    };

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        this.opponentModel = new opModel(utilitySpace, timeline);
        int numBids = 50;
        bidList = new ArrayList<>(numBids);
        for (int i=0; i<numBids; i++) {
            Bid tempBid = generateRandomBid();
            bidList.add(tempBid);
        }
        Collections.sort(bidList, bidComparator);
        System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
        System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {
        try {
            if (lastReceivedBid != null) {
                Double lastReceivedBidUtility = getUtilitySpace().getUtility(lastReceivedBid);
                if(lastReceivedBidUtility >= thresholdBidUtility || timeline.getTime() >= thresholdTime) {
                    return new Accept(getPartyId(), lastReceivedBid);
                }
                else {
                    return new Offer(getPartyId(), createBid());
                }
            } else if (timeline.getTime() >= thresholdTime) {
                return new Accept(getPartyId(), lastReceivedBid);
            } else {
                return new Offer(getPartyId(), createBid());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
            opponentModel.updateModel(lastReceivedBid);
        }
    }

    @Override
    public String getDescription() {
        return "Follows Nash Bargaining Solution: max(opponentUtility * agentUtility)";
    }

    public Bid createBid() {
        if (lastReceivedBid != null) {
            Bid offeredBid;
            if (Math.random() < 0.03) {
                offeredBid = generateRandomBid();
            } else {
                double maxBalance = 0;
                Bid maxbid = null;
                for (Bid bid : bidList) {
                    // NBS formula
                    double prod = opponentModel.getUtility(bid) * getUtilitySpace().getUtility(bid);
                    double sum = opponentModel.getUtility(bid) + getUtilitySpace().getUtility(bid);
                    Bid util1 = null, util2 = null;
                    try{
                        util1 = utilitySpace.getMaxUtilityBid();
                    } catch (Exception e) {
                        System.out.println("Discount Factor is ");
                    }
                    try{
                        util2 = utilitySpace.getMaxUtilityBid();
                    } catch (Exception e) {
                        System.out.println("Discount Factor is ");
                    }
                    double maxUtility = getUtilitySpace().getUtility(util1);
                    double oppMaxUtility = opponentModel.getUtility( util2);
                    double normalizedProd = prod / (maxUtility * oppMaxUtility);
                    double normalizedSum = sum / (maxUtility + oppMaxUtility);
                    double balance = 0.6 * normalizedProd + 0.4 * normalizedSum;
                    if (balance > maxBalance) {
                        maxBalance = balance;
                        maxbid = bid;
                    }
                }
                offeredBid = maxbid;
            }

            try {
                return (offeredBid != null) ? offeredBid : utilitySpace.getMaxUtilityBid();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        else {
            // if first bid, then offer maximum utility
            Bid offeredBid = null;
            try {
                offeredBid = new Bid(getUtilitySpace().getMaxUtilityBid());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return offeredBid;
        }
    }

}
