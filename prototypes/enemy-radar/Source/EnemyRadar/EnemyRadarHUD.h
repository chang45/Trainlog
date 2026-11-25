// 敌人位置标记原型 —— 屏幕标记绘制
// 把 DrawHUD 里的内容照搬到你自己的 HUD 类也可以，不依赖这个类。

#pragma once

#include "CoreMinimal.h"
#include "GameFramework/HUD.h"
#include "EnemyRadarComponent.h"
#include "EnemyRadarHUD.generated.h"

/** 屏幕标记：目视红菱形、听声蓝圆圈、屏幕外边缘箭头、右下角小地图 */
UCLASS()
class AEnemyRadarHUD : public AHUD
{
    GENERATED_BODY()

public:
    virtual void DrawHUD() override;

protected:
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Markers")
    FLinearColor SightColor = FLinearColor(1.f, 0.22f, 0.18f, 1.f);

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Markers")
    FLinearColor SoundColor = FLinearColor(0.30f, 0.85f, 1.f, 1.f);

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Markers", meta = (ClampMin = "4.0"))
    float MarkerSize = 11.f;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Markers")
    bool bShowDistance = true;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Markers")
    bool bDrawOffscreenArrows = true;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Minimap")
    bool bDrawMinimap = true;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Minimap", meta = (ClampMin = "40.0"))
    float MinimapRadius = 100.f;

    /** 小地图半径对应的世界距离（厘米） */
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Minimap", meta = (ClampMin = "500.0"))
    float MinimapWorldRadius = 3500.f;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Enemy Radar|Minimap")
    float MinimapMargin = 40.f;

private:
    UEnemyRadarComponent* FindRadar();

    void DrawContactMarker(const FEnemyContact& Contact, const FVector2D& ScreenPosition, const FLinearColor& Color, float UiScale, bool bSeen);
    void DrawEdgeArrow(const FEnemyContact& Contact, const FVector& WorldPosition, const FLinearColor& Color, float UiScale);
    void DrawMinimap(UEnemyRadarComponent* RadarComponent, float UiScale);

    TWeakObjectPtr<UEnemyRadarComponent> Radar;
};
