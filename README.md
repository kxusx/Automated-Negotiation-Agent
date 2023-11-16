# Automatic Negotiation Agent

Our agent follows the Nash Bargaining Solution approach towards finding the optimal agreement point. We also take into consideration the total social welfare measure of all the negotiating agents by creating a opponent frequency model to learn and predict opponent moves.

The formula we have used is as follows:

- NBS utility = *agent_utility_for_current_bid* such that *agent_utility_for_current_bid * opponent_agent_utility_for_current_bid* is maximised for all potential bids of our agent at a given turn.
- Social Welfare utility = *agent_utility_for_current_bid* such that *agent_utility_for_current_bid + opponent_agent_utility_for_current_bid* is maximised for all potential bids of our agent at a given turn.
- The above utilities are normalised by considering the maximum possible utilities for each agent in the utility space.
- Bid proposed at a given turn = 0.6 * (normalised NBS utility) + 0.4 * (normalised Social Welfare utility).

We have also taken into account two thresholds above which the agent will accept the opponent’s bid:

1. Threshold bid utility value = 0.85.
2. Threshold time = 0.98.