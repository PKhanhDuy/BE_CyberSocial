from typing import Literal

from pydantic import BaseModel, Field


RiskLevel = Literal["LOW", "MEDIUM", "HIGH"]


class UserProfileFeatures(BaseModel):
    logFollowers: float | None = None
    logFollowing: float | None = None
    logStatuses: float | None = None
    accountCreatedUnix: float | None = None
    hasProfile: float | None = None


class TreeFeatures(BaseModel):
    depth: float | None = None
    rootOutDegree: float | None = None


class CascadeFeatures(BaseModel):
    speed: float | None = None
    lifespan: float | None = None
    burstiness: float | None = None


class InteractionEvent(BaseModel):
    eventId: str | None = None
    parentEventId: str | None = None
    srcUserId: int = Field(default=0, ge=0)
    dstArticleId: int = Field(default=0, ge=0)
    timestampUnix: float | None = None
    eventOrder: float | None = None
    eventType: Literal["tweet", "retweet", "reply", "quote", "like", "comment", "share"] = "tweet"
    retweetCount: float = Field(default=0.0, ge=0)
    favoriteCount: float = Field(default=0.0, ge=0)
    textLen: float | None = None
    actorDisplayName: str | None = None
    userProfile: UserProfileFeatures | None = None
    tree: TreeFeatures | None = None
    cascade: CascadeFeatures | None = None


class AnalyzeRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=10000)
    articleId: int = Field(default=0, ge=0)
    events: list[InteractionEvent] = Field(default_factory=list)
    includeXai: bool = True


class TokenAttribution(BaseModel):
    token: str
    score: float


class GraphAttribution(BaseModel):
    feature: str
    score: float
    sharePercent: float


class EventAttribution(BaseModel):
    eventIndex: int
    eventType: str
    eventTypeLabel: str | None = None
    relativeTime: str | None = None
    actorLabel: str | None = None
    tigeRemoval: float | None = None
    confidenceDrop: float | None = None
    summary: str | None = None


class PropagationTimelineEvent(BaseModel):
    eventIndex: int
    eventId: str | None = None
    parentEventId: str | None = None
    depth: int = 0
    relativeTime: str
    eventType: str
    eventTypeLabel: str
    actorLabel: str
    tigeRemoval: float | None = None
    isInfluential: bool = False


class AnalyzeResponse(BaseModel):
    fakeProbability: float
    explanation: str
    riskLevel: RiskLevel
    label: Literal["REAL", "FAKE"]
    threshold: float
    eventCount: int = 0
    tokenAttributions: list[TokenAttribution] = Field(default_factory=list)
    graphAttributions: list[GraphAttribution] = Field(default_factory=list)
    eventAttributions: list[EventAttribution] = Field(default_factory=list)
    propagationTimeline: list[PropagationTimelineEvent] = Field(default_factory=list)
    graphContribution: float | None = None
    mode: Literal["full", "text_only_fallback"] = "full"


class HealthResponse(BaseModel):
    status: Literal["ok"]
    modelLoaded: bool
    device: str
    modelPath: str
    threshold: float
    edgeNormalization: bool
    stModelName: str
    numUsers: int | None = None
    warnings: list[str] = Field(default_factory=list)
