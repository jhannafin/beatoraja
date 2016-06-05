package bms.player.beatoraja.result;

import java.util.logging.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.beatoraja.skin.SkinImage;

public class GradeResult extends MainState {
	
	// TODO 段位リプレイの保存
	// TODO 段位ゲージ繊維の表示

	private static final String[] LAMP = { "000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
			"CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

	private MainController main;

	private BitmapFont titlefont;
	private String title;

	private PlayerResource resource;

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

    private MusicResultSkin skin;

	public GradeResult(MainController main) {
		this.main = main;
		
        skin = new MusicResultSkin();
		this.setSkin(skin);
	}

	public void create(PlayerResource resource) {
		this.resource = resource;
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + parameter.characters + "段位認定 " + resource.getCoursetitle() + "不合格";
		titlefont = generator.generateFont(parameter);
		updateScoreDatabase();
	}

	public void render() {
		int time = getNowTime();
		final SpriteBatch sprite = main.getSpriteBatch();
		IRScoreData score = resource.getCourseScoreData();

		if (score != null) {
			if (score.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
				Gdx.gl.glClearColor(0, 0, 0.4f, 1);
			} else {
				Gdx.gl.glClearColor(0.4f, 0, 0, 1);
			}
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			final float w = 1280;
			final float h = 720;

			sprite.begin();
			if (score != null) {
				titlefont.setColor(Color.WHITE);
				titlefont.draw(sprite, resource.getCoursetitle()
						+ (score.getClear() > GrooveGauge.CLEARTYPE_FAILED ? "  合格" : "  不合格"), w * 3 / 4, h / 2);
			}
			for(SkinImage img : skin.getSkinPart()) {
				if(img.getTiming() != 2) {
					img.draw(sprite, time);				
				}
			}

			if (score != null) {
				// totalnotes
				skin.getTotalnotes().draw(sprite, time, resource.getScoreData().getNotes());
				
				if (oldclear != 0) {
					titlefont.setColor(Color.valueOf(LAMP[oldclear]));
					titlefont.draw(sprite, CLEAR[oldclear] + " -> ", 240, 425);
				}
				titlefont.setColor(Color.valueOf(LAMP[score.getClear()]));
				titlefont.draw(sprite, CLEAR[score.getClear()], 440, 425);
				titlefont.setColor(Color.WHITE);

				if (oldexscore != 0) {
					titlefont.setColor(Color.WHITE);
					titlefont.draw(sprite, " -> ", 360, 395);
					skin.getScore(score.getExscore() > oldexscore ? 2 : 3).draw(sprite, time, Math.abs(score.getExscore() - oldexscore));
				}

				if (oldmisscount < 65535) {
					titlefont.draw(sprite, " -> ", 360, 365);
					skin.getMisscount(score.getMinbp() > oldmisscount ? 3 : 2).draw(sprite, time, Math.abs(score.getMinbp() - oldmisscount));
				}
				
				if(oldcombo > 0) {
					titlefont.draw(sprite, " -> ", 360, 335);
					skin.getMaxcombo(score.getCombo() > oldcombo ? 2 : 3).draw(sprite, time, Math.abs(score.getCombo() - oldcombo));
				}

				titlefont.draw(sprite, "FAST / SLOW  :  ", 100, 100);

				skin.getJudgeCount(true).draw(sprite, time,
						score.getFgr() + score.getFgd() + score.getFbd() + score.getFpr() + score.getFms());
				skin.getJudgeCount(false).draw(sprite, time,
						score.getSgr() + score.getSgd() + score.getSbd() + score.getSpr() + score.getSms());
			}
            
    		skin.getJudgeCount(true).draw(sprite, time, score.getFgr() + score.getFgd() + score.getFbd() + score.getFpr() + score.getFms());
    		skin.getJudgeCount(false).draw(sprite, time, score.getSgr() + score.getSgd() + score.getSbd() + score.getSpr() + score.getSms());
			sprite.end();
		}
		boolean[] keystate = main.getInputProcessor().getKeystate();
		long[] keytime = main.getInputProcessor().getTime();
		if (score == null
				|| ((System.currentTimeMillis() > time + 500 && (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
			keytime[0] = keytime[2] = keytime[4] = keytime[6] = 0;
			main.changeState(MainController.STATE_SELECTMUSIC);
		}
	}

	public void updateScoreDatabase() {
		BMSModel[] models = resource.getCourseBMSModels();
		IRScoreData newscore = resource.getCourseScoreData();
		if (newscore == null) {
			return;
		}
		IRScoreData score = main.getPlayDataAccessor().readScoreData(models, resource.getConfig().getLnmode(),
				resource.getConfig().getRandom() == 1);
		if (score == null) {
			score = new IRScoreData();
		}
		boolean ln = false;
		for(BMSModel model : models) {
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;			
		}
		if (ln && resource.getConfig().getLnmode() == 2) {
			oldclear = score.getExclear();
		} else {
			oldclear = score.getClear();
		}
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		oldcombo = score.getCombo();

		main.getPlayDataAccessor().writeScoreDara(newscore, models, resource.getConfig().getLnmode(),
				resource.getConfig().getRandom() == 1, resource.isUpdateScore());

		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}

	public int getJudgeCount(int judge, boolean fast) {
		IRScoreData score = resource.getCourseScoreData();
		if(score != null) {
			switch(judge) {
			case 0:
				return fast ? score.getFpg() : score.getSpg();
			case 1:
				return fast ? score.getFgr() : score.getSgr();
			case 2:
				return fast ? score.getFgd() : score.getSgd();
			case 3:
				return fast ? score.getFbd() : score.getSbd();
			case 4:
				return fast ? score.getFpr() : score.getSpr();
			case 5:
				return fast ? score.getFms() : score.getSms();
			}
		}
		return 0;
	}

	@Override
	public int getScore() {
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getExscore();			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetScore() {
		return oldexscore;
	}

	@Override
	public int getMaxcombo() {
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getCombo();			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetMaxcombo() {
		if(oldcombo > 0) {
			return oldcombo;			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getMisscount() {
		if(resource.getScoreData() != null) {
			return resource.getScoreData().getMinbp();			
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetMisscount() {
		return oldmisscount;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
