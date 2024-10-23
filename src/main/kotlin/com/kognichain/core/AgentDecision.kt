package com.kognichain.core;

sealed class AgentDecision

class ExecuteTaskDecision(val taskName: String, val parameters: Map<String, Any>? = null) : AgentDecision()
class StopAgentDecision : AgentDecision()
class CustomDecision(val details: String) : AgentDecision()
class RespondToUserDecision(val userResponse: String) : AgentDecision()