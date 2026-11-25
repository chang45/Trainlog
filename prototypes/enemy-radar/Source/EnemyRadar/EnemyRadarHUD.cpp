#include "EnemyRadarHUD.h"

#include "Camera/PlayerCameraManager.h"
#include "Engine/Canvas.h"
#include "Engine/Engine.h"
#include "Engine/Font.h"
#include "GameFramework/Pawn.h"
#include "GameFramework/PlayerController.h"

void AEnemyRadarHUD::DrawHUD()
{
    Super::DrawHUD();

    if (!Canvas || !PlayerOwner || !GetWorld())
    {
        return;
    }

    UEnemyRadarComponent* RadarComponent = FindRadar();
    if (!RadarComponent)
    {
        return;
    }

    const float UiScale = FMath::Clamp(Canvas->SizeY / 1080.f, 0.55f, 2.5f);
    const float Now = GetWorld()->GetTimeSeconds();
    const float Memory = FMath::Max(RadarComponent->GetMemoryDuration(), 0.1f);

    for (const FEnemyContact& Contact : RadarComponent->GetContactsRef())
    {
        const bool bSeen = Contact.LastSeenTime >= Contact.LastHeardTime;
        const float LastTime = FMath::Max(Contact.LastSeenTime, Contact.LastHeardTime);
        const float Age = Now - LastTime;
        if (LastTime < 0.f || Age > Memory)
        {
            continue;
        }

        const float Alpha = Contact.bVisibleNow ? 1.f : FMath::Clamp(1.f - Age / Memory, 0.18f, 1.f);
        FLinearColor Color = bSeen ? SightColor : SoundColor;
        Color.A *= Alpha;

        const FVector WorldPosition = Contact.LastKnownLocation + FVector(0.f, 0.f, RadarComponent->MarkerHeight);
        FVector2D ScreenPosition;
        const bool bProjected = PlayerOwner->ProjectWorldLocationToScreen(WorldPosition, ScreenPosition);
        const float Margin = 26.f * UiScale;
        const bool bOnScreen = bProjected
            && ScreenPosition.X > Margin && ScreenPosition.X < Canvas->SizeX - Margin
            && ScreenPosition.Y > Margin && ScreenPosition.Y < Canvas->SizeY - Margin;

        if (bOnScreen)
        {
            DrawContactMarker(Contact, ScreenPosition, Color, UiScale, bSeen);
        }
        else if (bDrawOffscreenArrows)
        {
            DrawEdgeArrow(Contact, WorldPosition, Color, UiScale);
        }
    }

    if (bDrawMinimap)
    {
        DrawMinimap(RadarComponent, UiScale);
    }
}

UEnemyRadarComponent* AEnemyRadarHUD::FindRadar()
{
    if (Radar.IsValid())
    {
        return Radar.Get();
    }

    if (PlayerOwner)
    {
        if (APawn* Pawn = PlayerOwner->GetPawn())
        {
            Radar = Pawn->FindComponentByClass<UEnemyRadarComponent>();
        }
    }
    return Radar.Get();
}

void AEnemyRadarHUD::DrawContactMarker(const FEnemyContact& Contact, const FVector2D& ScreenPosition, const FLinearColor& Color, float UiScale, bool bSeen)
{
    const float Radius = MarkerSize * UiScale;
    const float Thickness = FMath::Max(1.f, 1.8f * UiScale);

    if (bSeen)
    {
        // 菱形：目视确认的敌人
        DrawLine(ScreenPosition.X, ScreenPosition.Y - Radius, ScreenPosition.X + Radius, ScreenPosition.Y, Color, Thickness);
        DrawLine(ScreenPosition.X + Radius, ScreenPosition.Y, ScreenPosition.X, ScreenPosition.Y + Radius, Color, Thickness);
        DrawLine(ScreenPosition.X, ScreenPosition.Y + Radius, ScreenPosition.X - Radius, ScreenPosition.Y, Color, Thickness);
        DrawLine(ScreenPosition.X - Radius, ScreenPosition.Y, ScreenPosition.X, ScreenPosition.Y - Radius, Color, Thickness);
        DrawLine(ScreenPosition.X, ScreenPosition.Y - Radius * 0.35f, ScreenPosition.X, ScreenPosition.Y + Radius * 0.35f, Color, Thickness * 0.7f);
    }
    else
    {
        // 圆圈：只有声源，位置是最后一次听到的地方
        const int32 Segments = 16;
        for (int32 Index = 0; Index < Segments; ++Index)
        {
            const float Angle0 = 2.f * PI * Index / Segments;
            const float Angle1 = 2.f * PI * (Index + 1) / Segments;
            DrawLine(
                ScreenPosition.X + FMath::Cos(Angle0) * Radius * 0.9f,
                ScreenPosition.Y + FMath::Sin(Angle0) * Radius * 0.9f,
                ScreenPosition.X + FMath::Cos(Angle1) * Radius * 0.9f,
                ScreenPosition.Y + FMath::Sin(Angle1) * Radius * 0.9f,
                Color, Thickness);
        }
    }

    FString Label = bSeen ? TEXT("敌人") : TEXT("声源");
    if (Contact.Target.IsValid())
    {
        FString RawName = Contact.Target->GetName();
        RawName.RemoveFromEnd(TEXT("_C"));
        Label = RawName;
    }
    if (bShowDistance)
    {
        Label += FString::Printf(TEXT("  %dm"), FMath::RoundToInt(Contact.Distance / 100.f));
    }

    UFont* Font = GEngine ? GEngine->GetSmallFont() : nullptr;
    const float TextScale = 0.9f * UiScale;
    float TextWidth = 0.f;
    float TextHeight = 0.f;
    GetTextSize(Label, TextWidth, TextHeight, Font, TextScale);
    DrawText(Label, Color, ScreenPosition.X - TextWidth * 0.5f, ScreenPosition.Y - Radius - TextHeight - 4.f * UiScale, Font, TextScale);
}

void AEnemyRadarHUD::DrawEdgeArrow(const FEnemyContact& Contact, const FVector& WorldPosition, const FLinearColor& Color, float UiScale)
{
    const FVector2D Center(Canvas->SizeX * 0.5f, Canvas->SizeY * 0.5f);
    FVector2D Direction(0.f, -1.f);

    FVector2D Projected;
    if (PlayerOwner->ProjectWorldLocationToScreen(WorldPosition, Projected))
    {
        const FVector2D Offset = Projected - Center;
        if (!Offset.IsNearlyZero())
        {
            Direction = Offset.GetSafeNormal();
        }
    }
    else if (PlayerOwner->PlayerCameraManager)
    {
        const APlayerCameraManager* CameraManager = PlayerOwner->PlayerCameraManager;
        const FVector Delta = (WorldPosition - CameraManager->GetCameraLocation()).GetSafeNormal();
        const FVector Local = CameraManager->GetCameraRotation().UnrotateVector(Delta);
        Direction = FVector2D(Local.Y, -Local.Z);
        if (Direction.IsNearlyZero())
        {
            Direction = FVector2D(0.f, -1.f);
        }
        Direction.Normalize();
    }
    else
    {
        return;
    }

    // 目标在屏幕外时，箭头贴在椭圆边界上，屏幕比例变化也不会跑出画面
    const float RadiusX = Canvas->SizeX * 0.5f - 90.f * UiScale;
    const float RadiusY = Canvas->SizeY * 0.5f - 110.f * UiScale;
    const float Angle = FMath::Atan2(Direction.Y, Direction.X);
    const FVector2D Position = Center + FVector2D(FMath::Cos(Angle) * RadiusX, FMath::Sin(Angle) * RadiusY);

    const float Size = 9.f * UiScale;
    const FVector2D Tip = Position + Direction * (Size * 1.6f);
    const FVector2D Perpendicular(-Direction.Y, Direction.X);
    const FVector2D Base = Position - Direction * Size;
    const FVector2D Left = Base + Perpendicular * Size * 0.8f;
    const FVector2D Right = Base - Perpendicular * Size * 0.8f;

    const float Thickness = FMath::Max(1.f, 1.8f * UiScale);
    DrawLine(Tip.X, Tip.Y, Left.X, Left.Y, Color, Thickness);
    DrawLine(Tip.X, Tip.Y, Right.X, Right.Y, Color, Thickness);
    DrawLine(Left.X, Left.Y, Right.X, Right.Y, Color, Thickness * 0.6f);

    if (bShowDistance)
    {
        UFont* Font = GEngine ? GEngine->GetSmallFont() : nullptr;
        const float TextScale = 0.8f * UiScale;
        const FString Label = FString::Printf(TEXT("%dm"), FMath::RoundToInt(Contact.Distance / 100.f));
        float TextWidth = 0.f;
        float TextHeight = 0.f;
        GetTextSize(Label, TextWidth, TextHeight, Font, TextScale);
        DrawText(Label, Color, Position.X - TextWidth * 0.5f, Position.Y + Size * 1.8f, Font, TextScale);
    }
}

void AEnemyRadarHUD::DrawMinimap(UEnemyRadarComponent* RadarComponent, float UiScale)
{
    APawn* Pawn = PlayerOwner ? PlayerOwner->GetPawn() : nullptr;
    if (!Pawn)
    {
        return;
    }

    const float Radius = MinimapRadius * UiScale;
    const float Margin = MinimapMargin * UiScale;
    const FVector2D Center(Canvas->SizeX - Radius - Margin, Canvas->SizeY - Radius - Margin);

    DrawRect(FLinearColor(0.02f, 0.04f, 0.06f, 0.55f), Center.X - Radius, Center.Y - Radius, Radius * 2.f, Radius * 2.f);

    const FLinearColor Rim(0.45f, 0.75f, 0.9f, 0.35f);
    const int32 Segments = 48;
    for (int32 Index = 0; Index < Segments; ++Index)
    {
        const float Angle0 = 2.f * PI * Index / Segments;
        const float Angle1 = 2.f * PI * (Index + 1) / Segments;
        DrawLine(Center.X + FMath::Cos(Angle0) * Radius, Center.Y + FMath::Sin(Angle0) * Radius,
            Center.X + FMath::Cos(Angle1) * Radius, Center.Y + FMath::Sin(Angle1) * Radius, Rim, 1.f);
    }

    for (int32 Ring = 1; Ring <= 2; ++Ring)
    {
        const float RingRadius = Radius * (Ring / 3.f);
        const FLinearColor RingColor(0.45f, 0.75f, 0.9f, 0.12f);
        for (int32 Index = 0; Index < Segments; Index += 2)
        {
            const float Angle0 = 2.f * PI * Index / Segments;
            const float Angle1 = 2.f * PI * (Index + 1) / Segments;
            DrawLine(Center.X + FMath::Cos(Angle0) * RingRadius, Center.Y + FMath::Sin(Angle0) * RingRadius,
                Center.X + FMath::Cos(Angle1) * RingRadius, Center.Y + FMath::Sin(Angle1) * RingRadius, RingColor, 1.f);
        }
    }

    const float Sweep = FMath::Fmod(GetWorld()->GetTimeSeconds() * 1.1f, 2.f * PI);
    DrawLine(Center.X, Center.Y, Center.X + FMath::Cos(Sweep) * Radius, Center.Y + FMath::Sin(Sweep) * Radius,
        FLinearColor(0.35f, 0.9f, 1.f, 0.18f), 1.5f);

    // 玩家箭头固定朝上，小地图随相机朝向旋转
    const FLinearColor PlayerColor(0.8f, 1.f, 0.85f, 0.9f);
    DrawLine(Center.X, Center.Y + 7.f * UiScale, Center.X + 6.f * UiScale, Center.Y - 6.f * UiScale, PlayerColor, 2.f);
    DrawLine(Center.X, Center.Y + 7.f * UiScale, Center.X - 6.f * UiScale, Center.Y - 6.f * UiScale, PlayerColor, 2.f);
    DrawLine(Center.X + 6.f * UiScale, Center.Y - 6.f * UiScale, Center.X - 6.f * UiScale, Center.Y - 6.f * UiScale, PlayerColor, 2.f);

    const float Yaw = PlayerOwner->PlayerCameraManager
        ? PlayerOwner->PlayerCameraManager->GetCameraRotation().Yaw
        : Pawn->GetActorRotation().Yaw;
    const FVector PlayerLocation = Pawn->GetActorLocation();
    const float Now = GetWorld()->GetTimeSeconds();
    const float Memory = FMath::Max(RadarComponent->GetMemoryDuration(), 0.1f);

    for (const FEnemyContact& Contact : RadarComponent->GetContactsRef())
    {
        const float LastTime = FMath::Max(Contact.LastSeenTime, Contact.LastHeardTime);
        const float Age = Now - LastTime;
        if (LastTime < 0.f || Age > Memory)
        {
            continue;
        }

        const FVector Local = (Contact.LastKnownLocation - PlayerLocation).RotateAngleAxis(-Yaw, FVector::UpVector);
        const FVector2D Offset(Local.Y / MinimapWorldRadius * Radius, -Local.X / MinimapWorldRadius * Radius);
        if (Offset.Size() > Radius - 3.f * UiScale)
        {
            continue;
        }

        const FVector2D Position = Center + Offset;
        const bool bSeen = Contact.LastSeenTime >= Contact.LastHeardTime;
        FLinearColor Color = bSeen ? SightColor : SoundColor;
        Color.A = Contact.bVisibleNow ? 1.f : FMath::Clamp(1.f - Age / Memory, 0.2f, 1.f);

        const float DotSize = (bSeen ? 5.f : 3.5f) * UiScale;
        DrawRect(Color, Position.X - DotSize * 0.5f, Position.Y - DotSize * 0.5f, DotSize, DotSize);
    }
}
