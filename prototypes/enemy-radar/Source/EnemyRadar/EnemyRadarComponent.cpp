#include "EnemyRadarComponent.h"

#include "Camera/CameraComponent.h"
#include "DrawDebugHelpers.h"
#include "Engine/World.h"
#include "Kismet/GameplayStatics.h"

UEnemyRadarComponent::UEnemyRadarComponent()
{
    PrimaryComponentTick.bCanEverTick = true;
    PrimaryComponentTick.TickGroup = TG_PostPhysics;
}

void UEnemyRadarComponent::BeginPlay()
{
    Super::BeginPlay();

    if (AActor* Owner = GetOwner())
    {
        CachedCamera = Owner->FindComponentByClass<UCameraComponent>();
    }
    RefreshTargets();
}

void UEnemyRadarComponent::TickComponent(float DeltaTime, ELevelTick TickType, FActorComponentTickFunction* ThisTickFunction)
{
    Super::TickComponent(DeltaTime, TickType, ThisTickFunction);

    if (!GetOwner() || !GetWorld())
    {
        return;
    }

    RefreshAccumulator += DeltaTime;
    if (RefreshAccumulator >= TargetRefreshInterval)
    {
        RefreshAccumulator = 0.f;
        RefreshTargets();
    }

    ScanAccumulator += DeltaTime;
    if (ScanAccumulator >= ScanInterval)
    {
        ScanAccumulator = 0.f;
        ScanTargets();
    }

    if (bDrawDebug)
    {
        const float Now = GetWorld()->GetTimeSeconds();
        const FVector Eye = GetEyeLocation();
        for (const FEnemyContact& Contact : Contacts)
        {
            AActor* Target = Contact.Target.Get();
            if (!Target)
            {
                continue;
            }

            const float Age = Now - FMath::Max(Contact.LastSeenTime, Contact.LastHeardTime);
            const FColor LineColor = Age > 0.35f ? FColor(110, 110, 110) : (Contact.bVisibleNow ? FColor::Red : FColor::Cyan);
            DrawDebugLine(GetWorld(), Eye, Target->GetActorLocation(), LineColor, false, 0.f, 0, 1.5f);
            DrawDebugSphere(GetWorld(), Contact.LastKnownLocation + FVector(0.f, 0.f, MarkerHeight), 16.f, 8, LineColor, false, 0.f, 0, 1.f);
        }
    }
}

void UEnemyRadarComponent::SetTargets(const TArray<AActor*>& InTargets)
{
    ManualTargets.Reset();
    for (AActor* Actor : InTargets)
    {
        if (IsValid(Actor) && Actor != GetOwner())
        {
            ManualTargets.Add(Actor);
        }
    }
    RefreshTargets();
}

void UEnemyRadarComponent::AddTarget(AActor* InTarget)
{
    if (IsValid(InTarget) && InTarget != GetOwner())
    {
        ManualTargets.AddUnique(InTarget);
        RefreshTargets();
    }
}

void UEnemyRadarComponent::ClearTargets()
{
    ManualTargets.Reset();
    CachedTargets.Reset();
    Contacts.Reset();
}

TArray<FEnemyContact> UEnemyRadarComponent::GetContacts() const
{
    return Contacts;
}

FVector UEnemyRadarComponent::GetEyeLocation()
{
    if (bUseCameraForSight)
    {
        if (!CachedCamera.IsValid())
        {
            if (AActor* Owner = GetOwner())
            {
                CachedCamera = Owner->FindComponentByClass<UCameraComponent>();
            }
        }
        if (CachedCamera.IsValid())
        {
            return CachedCamera->GetComponentLocation();
        }
    }

    if (AActor* Owner = GetOwner())
    {
        return Owner->GetActorLocation() + FVector(0.f, 0.f, 64.f);
    }
    return FVector::ZeroVector;
}

FVector UEnemyRadarComponent::GetEyeForward()
{
    if (bUseCameraForSight)
    {
        if (!CachedCamera.IsValid())
        {
            if (AActor* Owner = GetOwner())
            {
                CachedCamera = Owner->FindComponentByClass<UCameraComponent>();
            }
        }
        if (CachedCamera.IsValid())
        {
            return CachedCamera->GetForwardVector();
        }
    }

    if (AActor* Owner = GetOwner())
    {
        return Owner->GetActorForwardVector();
    }
    return FVector::ForwardVector;
}

void UEnemyRadarComponent::RefreshTargets()
{
    CachedTargets.Reset();

    if (ManualTargets.Num() > 0)
    {
        for (const TWeakObjectPtr<AActor>& Target : ManualTargets)
        {
            if (Target.IsValid())
            {
                CachedTargets.Add(Target);
            }
        }
        return;
    }

    if (EnemyActorTag.IsNone())
    {
        return;
    }

    TArray<AActor*> Found;
    UGameplayStatics::GetAllActorsWithTag(this, EnemyActorTag, Found);
    for (AActor* Actor : Found)
    {
        if (IsValid(Actor) && Actor != GetOwner())
        {
            CachedTargets.Add(Actor);
        }
    }
}

void UEnemyRadarComponent::ScanTargets()
{
    UWorld* World = GetWorld();
    AActor* Owner = GetOwner();
    if (!World || !Owner)
    {
        return;
    }

    const float Now = World->GetTimeSeconds();
    const FVector Eye = GetEyeLocation();
    const FVector Forward = GetEyeForward();

    for (const TWeakObjectPtr<AActor>& Cached : CachedTargets)
    {
        AActor* Target = Cached.Get();
        if (!IsValid(Target) || Target == Owner)
        {
            continue;
        }

        float Distance = 0.f;
        const bool bVisible = ComputeSeesActor(Target, Eye, Forward, Distance);
        const bool bHeard = !bVisible && ComputeHearsActor(Target, Eye, Distance);

        FEnemyContact* Contact = Contacts.FindByPredicate([Target](const FEnemyContact& Item)
        {
            return Item.Target.Get() == Target;
        });

        const bool bWasVisible = Contact && Contact->bVisibleNow;
        if (!Contact)
        {
            Contact = &Contacts.AddDefaulted_GetRef();
            Contact->Target = Target;
        }

        Contact->Distance = Distance;
        Contact->bVisibleNow = bVisible;

        if (bVisible)
        {
            Contact->LastSeenTime = Now;
            Contact->LastKnownLocation = Target->GetActorLocation();
        }
        else if (bHeard)
        {
            Contact->LastHeardTime = Now;
            Contact->LastKnownLocation = Target->GetActorLocation();
        }

        if (bVisible && !bWasVisible)
        {
            OnContactAdded.Broadcast(*Contact);
        }
    }

    // 目标被销毁，或最后看到/听到的时间超过记忆时长，就清掉联系
    const float Memory = MemoryDuration;
    Contacts.RemoveAll([Now, Memory](const FEnemyContact& Item)
    {
        if (!Item.Target.IsValid())
        {
            return true;
        }
        const float LastTime = FMath::Max(Item.LastSeenTime, Item.LastHeardTime);
        return (Now - LastTime) > Memory;
    });
}

bool UEnemyRadarComponent::ComputeSeesActor(const AActor* Target, const FVector& Eye, const FVector& Forward, float& OutDistance) const
{
    const FVector TargetLocation = Target->GetActorLocation() + FVector(0.f, 0.f, MarkerHeight * 0.6f);
    const FVector Delta = TargetLocation - Eye;
    OutDistance = Delta.Size();

    if (OutDistance > SightRange)
    {
        return false;
    }

    // 只按水平方向判断视角，避免相机俯仰把近处敌人挡在锥形外
    FVector Delta2D(Delta.X, Delta.Y, 0.f);
    FVector Forward2D(Forward.X, Forward.Y, 0.f);
    if (!Delta2D.Normalize() || !Forward2D.Normalize())
    {
        return false;
    }
    if (FVector::DotProduct(Delta2D, Forward2D) < FMath::Cos(FMath::DegreesToRadians(SightHalfAngle)))
    {
        return false;
    }

    if (!bRequireLineOfSight)
    {
        return true;
    }

    FCollisionQueryParams Params(SCENE_QUERY_STAT(EnemyRadarSight), false, GetOwner());
    Params.AddIgnoredActor(Target);

    FHitResult Hit;
    const bool bBlocked = GetWorld()->LineTraceSingleByChannel(Hit, Eye, TargetLocation, SightTraceChannel, Params);
    return !bBlocked;
}

bool UEnemyRadarComponent::ComputeHearsActor(const AActor* Target, const FVector& Eye, float& OutDistance) const
{
    OutDistance = FVector::Dist(Eye, Target->GetActorLocation());
    if (OutDistance > HearingRadius)
    {
        return false;
    }
    return Target->GetVelocity().SizeSquared2D() >= FMath::Square(MinNoiseSpeed);
}
