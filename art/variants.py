"""Writes the models of every variant of the scene copies: each is one of the Blockbench models in
models/diegesis/<furniture>/ with the textures Refurbished gives that variant (its wood, colour or finish).

    python art/variants.py
"""
import json
import pathlib

MODELS = pathlib.Path(__file__).resolve().parent.parent / 'common/src/main/resources/assets/handson_furniture/models/diegesis'

KITCHEN = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'crimson', 'warped', 'white', 'orange',
           'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red',
           'black']

# furniture: (variants, Refurbished's texture name after the variant, the parts)
FURNITURE = {
    'fridge': (['light', 'dark'], 'fridge', ['body', 'door']),
    'freezer': (['light', 'dark'], 'fridge', ['body', 'door']),
    'kitchen_cabinet': (KITCHEN, 'kitchen_storage_cabinet', ['body_left', 'body_right', 'door_left', 'door_right']),
    'kitchen_drawer': (KITCHEN, 'kitchen_drawer', ['body', 'tray', 'tray_lower']),
}


def main():
    written = 0
    for furniture, (variants, texture, parts) in FURNITURE.items():
        folder = MODELS / furniture
        for old in folder.glob('*.json'):
            if old.stem not in parts:
                old.unlink()
        for variant in variants:
            for part in parts:
                model = {
                    'parent': f'handson_furniture:diegesis/{furniture}/{part}',
                    'textures': {
                        'particle': f'refurbished_furniture:block/{variant}_particle',
                        'texture': f'refurbished_furniture:block/{variant}_{texture}',
                    },
                }
                (folder / f'{variant}_{part}.json').write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8', newline='\n')
                written += 1
    print(f'{written} variant models')


if __name__ == '__main__':
    main()
